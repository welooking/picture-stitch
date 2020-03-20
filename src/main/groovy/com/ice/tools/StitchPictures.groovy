package com.ice.tools
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat

//代码功能说明 2020-03-14
/*
1、扫描指定目录，将目录下所有非 thumb.jpg 结尾的 jpg 图片文件拼接成一个大图
2、默认拼接为3行，如果图片数量不能整除3，多余的会被丢弃
4、拼接时为了整齐，会将所有横排的拼一列，竖排的拼一列
5、先每3张类型一致（横排或者竖排）的图片竖着拼起来，然后再把所有拼好的竖拼图片横着拼起来，保证最终出的图不会出现大黑块
6、3张竖拼时，会对图片做缩放，依据是3张图片的高度不超过1080像素。
7、最终拼接时还会做一次调整，因为三张竖排和三张横排的高度肯定不一致，调整时会向上调整到高度最大的

todolist:
1、需要能自定义行数
2、行数根据总图片数自动定义，比如大于40的就5行，大于20的3行
3、给定目录后自动检索子目录中的图片
 */

//全局变量：目标目录和输出目录；拼接图片最大高度
def config = new ConfigSlurper("configure").parse(new File('src/main/resources/Configure.groovy').toURL())
sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
targetFolder=config.targetFolder?new File(config.targetFolder):new File("src/main/resources")
outputFolder=config.outputFolder?new File(config.outputFolder):new File("output")
if (!outputFolder.exists()) outputFolder.mkdirs()
maxHeigh=config.maxHeigh?config.maxHeigh:1050
main()

def log(event){
    println "${sdf.format(new Date())} ${event}."
}
//扫描目标目录下所有子目录中的文件，根据文件数量的不同，设定不同的行数。
//当前只处理两种情况：1、targetFolder 下没有其他任何目录；2、targetFolder下有子目录，子目录下还有子目录，图片在最下一层的目录里
def main(){
	log "Pictures folder is '${targetFolder}', output to '${outputFolder}'"
	//扫描指定目录下的二级目录
	def dirCount=0
	targetFolder.eachDir{
		dirCount++
		it.eachDir{
			dirCount++
			scanPicture(it)
		}
	}
	if (dirCount==0) {
		scanPicture(targetFolder)
	}else{
		log "Total folder is:${dirCount}"
	}
	log "Complete"
}

def scanPicture(folder){
	//如果当前目录的拼接文件不存在
	def combineImg=new File(outputFolder.path+File.separator+"${folder.name}.jpg")
	if (!combineImg.exists()){
		def fileCount=folder.listFiles().size()
		if (fileCount<10){
			generateFolderImages(folder,2,maxHeigh)
		}else if (fileCount<20){
			generateFolderImages(folder,3,maxHeigh)
		}else if (fileCount<40){
			generateFolderImages(folder,4,maxHeigh)
		}else if (fileCount<100){
			generateFolderImages(folder,5,maxHeigh)
		}else if (fileCount<200){
			generateFolderImages(folder,6,maxHeigh)
		}else{
			log "error: too many images."
		}
	}
}

//根据指定的图片目录，行数和最大高度生成拼接图
def generateFolderImages(folder,row,height){
	def imgs=[] //所有高、宽相同的图片集
	def groupImgs=[] //所有做了纵向拼接的图片集
	def groupHorizontalImgs=[] //所有高小于宽的图片集，拼接后写入 groupImgs，然后清空准备下一次
	def groupVerticalImgs=[] //所有高大于款的图片集，拼接后写入 groupImgs，然后清空准备下一次
	folder.eachFile{
		//文件大小要大于1k
		if (it.size()>1024){
			//后缀是 jpg、jpeg、png、bmp，且不能是 thumb.jpg
			if (!it.name.toLowerCase().endsWith("thumb.jpg") &&
				 (it.name.toLowerCase().endsWith("jpg")
				|| it.name.toLowerCase().endsWith("jpeg")
				|| it.name.toLowerCase().endsWith("png")
				|| it.name.toLowerCase().endsWith("bmp"))){
				//等比例缩小图片
				try{
					def srcImg = ImageIO.read(it)
					if (srcImg){
						def scaleHeigh=height/row as int
						def scaleWidth=srcImg.width*scaleHeigh/srcImg.height as int
						//usage:BufferedImage(int width, int height, int imageType)
						def scaleImg = new BufferedImage(scaleWidth, scaleHeigh, BufferedImage.TYPE_INT_RGB);
						scaleImg.getGraphics().drawImage(srcImg, 0, 0, scaleWidth, scaleHeigh, null);
						//高宽不同的图片写入不同的集合
						if (srcImg.height>srcImg.width) groupVerticalImgs<<scaleImg
						else if (srcImg.height<srcImg.width) groupHorizontalImgs<<scaleImg
						else imgs<<scaleImg
						//根据行数选择图片纵向拼接
						if (groupVerticalImgs.size()!=0 && groupVerticalImgs.size().mod(row)==0) {
							groupImgs<<mergeImage(false,groupVerticalImgs)
							groupVerticalImgs=[]
						}
						if (groupHorizontalImgs.size()!=0 && groupHorizontalImgs.size().mod(row)==0) {
							groupImgs<<mergeImage(false,groupHorizontalImgs)
							groupHorizontalImgs=[]
						}
					}else{
						log "ERROR: not images file, ${it}"
					}
				}catch(Exception ex){
					log ex.message
				}
		}
		}
	}
	//达不到行数要求的图片会被丢弃。未来考虑填充处理
	//if (groupVerticalImgs.size()>0) mergeImage(false,groupVerticalImgs)
	//if (groupHorizontalImgs.size()>0) mergeImage(false,groupHorizontalImgs)

	//将所有纵向拼接的图片再横向拼接，然后写入新文件。
	if (groupImgs) generate(true,groupImgs,folder.name)
}

//将拼接好的图片写入指定文件。
def generate(flag,imgs,name){
	def imageNew=mergeImage(flag,imgs)
	if (imageNew) log "Combine image success, width:heigh => ${imageNew.getWidth()}:${imageNew.getHeight()}, save to '${name}.jpg'"
	else log "get null image data."
	//写图片中间的参数支持：png,jpg,gif；只支持如下几种类型的图像文件：jpg、jpeg、png、bmp"
	ImageIO.write(imageNew, "jpg", new File(outputFolder.path+File.separator+"${name}.jpg"));
	//ImageIO.write(imageNew, "合并后图片",Files.newOutputStream(Paths.get("final.jpg")))
}

/**
 * 合并任数量的图片成一张图片
 * @param isHorizontal：true代表水平合并，fasle代表垂直合并
 */
private BufferedImage mergeImage(boolean isHorizontal, imgs){
    // 生成新图片
    BufferedImage destImage = null;
    // 计算新图片的长和高
    int allw = 0, allh = 0, allwMax = 0, allhMax = 0;
    // 获取总长、总宽、最长、最宽
    imgs.each{
        BufferedImage img = it;
        allw += img.getWidth();
        allh += img.getHeight();
        if (img.getWidth() > allwMax) {
            allwMax = img.getWidth();
        }
        if (img.getHeight() > allhMax) {
            allhMax = img.getHeight();
        }
    }

    //在水平合并时，根据上面计算结果调整，对齐高度不一的图片
   	if (isHorizontal) {
   		allw=0 //重新计算总宽
    	imgs.each{
	        BufferedImage img = it;
    		def scaleWidth=img.getWidth()
    		if (img.height+20<allhMax){
				scaleWidth=img.width/img.height*allhMax as int
			}
			allw += scaleWidth;
    	}
    }
    // 创建新图片
    if (isHorizontal) {
        destImage = new BufferedImage(allw, allhMax, BufferedImage.TYPE_INT_RGB);
    } else {
        destImage = new BufferedImage(allwMax, allh, BufferedImage.TYPE_INT_RGB);
    }
    // 合并所有子图片到新图片
    int wx = 0, wy = 0;
    imgs.each{
        BufferedImage img = it;
    	//在水平合并时，如果图片的高度小于新图片最高高度，则先等比例放大，然后再处理
    	if (isHorizontal) {
    		if (img.height+20<allhMax){
				def scaleWidth=img.width/img.height*allhMax as int
				//def scaleWidth=img.width
				def scaleHeigh=allhMax as int
				def scaleImg = new BufferedImage(scaleWidth, scaleHeigh, BufferedImage.TYPE_INT_RGB);
				scaleImg.getGraphics().drawImage(img, 0, 0, scaleWidth, scaleHeigh, null);
				img=scaleImg
			}
    	}
        int w1 = img.getWidth();
        int h1 = img.getHeight();
        // 从图片中读取RGB
        int[] ImageArrayOne = new int[w1 * h1];
        ImageArrayOne = img.getRGB(0, 0, w1, h1, ImageArrayOne, 0, w1); // 逐行扫描图像中各个像素的RGB到数组中
        if (isHorizontal) { // 水平方向合并
            destImage.setRGB(wx, 0, w1, h1, ImageArrayOne, 0, w1); // 设置上半部分或左半部分的RGB
        } else { // 垂直方向合并
            destImage.setRGB(0, wy, w1, h1, ImageArrayOne, 0, w1); // 设置上半部分或左半部分的RGB
        }
        wx += w1;
        wy += h1;
    }
    return destImage
}