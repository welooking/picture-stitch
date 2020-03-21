Picture stitch - 将指定目录中的图片拼接成缩略图
====

## 功能简介 Introduction
1. 扫描指定目录，将目录下所有非 thumb.jpg 结尾的 jpg 图片文件拼接成一个大图
2. 自动根据目录中的图片数量调整，10张以内分2行，20张以内分3行，40张分4行，100张分5行，200张分6行，超过200张退出。
3. 拼接时为了整齐，会将所有宽大于高图片拼一列，高大于宽的拼一列
4. 先按指定数量将图片竖着拼起来，然后再把所有拼好的竖拼图片横拼，保证最终出的图不会出现空白
5. 竖拼时，会对图片做缩放，依据是3张图片的高度不超过配置的最大像素。
6. 最终拼接时还会做一次调整，因为竖排和横排的高度肯定不一致，调整时会向上调整到高度最大的

## 配置 config
图片目录、输出目录和图片的最终高度可以通过如下配置文件修改：
`src/main/resrouces/Configure.groovy`
```
//图片集目录
targetFolder="src/main/resources"
//输出目录
outputFolder="output"
//图片最大高度
maxHeigh=1050
```
## 使用 Usage
1. 安装gradle，centos下可以使用 `sdk install gradle`，windows下下载安装。
2. 运行：`gradle` 或者 `gradle runScript`
3. 拼接好的图片会显示在 output 目录下

效果如下：
![picture-stitch](http://img.welooking.cn/resources.jpg)

## License

The MIT License(http://opensource.org/licenses/MIT)

Please feel free to use and contribute to the development.

## Contribution

If you have any ideas or suggestions to improve Wechat WeUI, welcome to submit an issue/pull request.

