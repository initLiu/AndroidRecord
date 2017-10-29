* ## 画弧形或扇形
```java
//Canvas.java
public void drawArc(float left, float top, float right, float bottom, float startAngle,
            float sweepAngle, boolean useCenter, @NonNull Paint paint)
```
left,top,right,bottom是指弧形或扇形所在的椭圆左上角和右下角的坐标。如图所示：

![](./11.png)

startAngle是弧形的起始角度（x轴的正方向，即正右的方向，是0度的位置；顺时针为正角度，逆时针为负角度），sweepAngle是弧度划过的角度；
useCenter表示是否连接到圆心，如果不连接到圆心，就是弧形，如果连接到圆心，就是扇形。


* ## path中画弧线
```java
//Path.java
//forceMoveTo--->true新建一个子图形，forceMove---->false不新建子图形
//如果arc的起点和当前path的终点不一致，会制动调用lineTo()连接上两点。如果path为空，会调用moveTo()移动到arc的起点。
public void arcTo (float left, float top, float right, float bottom, float startAngle, float sweepAngle, boolean forceMoveTo)
```
```java
paint.setStyle(Style.STROKE);
paint.lineTo(100,100);
paint.arcTo(100,100,300,300,-90,90,true);//forceMoveTo为true，新建一个子图形，path为空，所以是调用moveTo()，不会自动连接之前的终点和arc的起点
```
![](./2.png)
```java
paint.setStyle(Style.STROKE);
paint.lineTo(100,100);
paint.arcTo(100,100,300,300,-90,90,false);//forceMoveTo为false，不会新建子图形，而是在原来的图形上画，因为调用lineTo()连接两点
```
![](./3.png)

* ## path中添加弧线
```java
//Path.java
public void addArc (float left, float top, float right, float bottom, float startAngle, float sweepAngle)
```
这个方法和arcTo相当于arcTo使用forceMoveTo=true

* ## path.close()封闭当前图形
```java
public void close ()
```
**它的作用是把当前的<font color='red'>子图形封闭</font>，即由当前位置向当前子图形的起点绘制一条直线。close()和lineTo(起点坐标)是完全等价的。
<font color='red'>[子图形]:官方文档叫做contour，在path中使用addxxx()方法的时候，每一次方法调用都是新增了一个独立的子图形；而如果使用xxxTo()等方法的时候，则是每一次短线（即每一次抬笔<font color='blue'>moveTo</font>），都标志着一个子图形的结束，以及一个新的子图形的开始。</font>
另外，不是所有的子图形都需要使用close()来封闭。当需要填充图形时（即Paint.Style为FILL或FILL_AND_STROKE）,path会自动封闭子图形**

*例子*
```java
//画心型
Path path = new Path();
path.addArc(200, 200, 400, 400, -225, 225);
path.arcTo(400, 200, 600, 400, -180, 225, false);
path.lineTo(400,542);
path.close();
```
![](./4.png)