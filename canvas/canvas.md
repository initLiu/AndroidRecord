## 画弧形或扇形
```java
public void drawArc(float left, float top, float right, float bottom, float startAngle,
            float sweepAngle, boolean useCenter, @NonNull Paint paint)
```
left,top,right,bottom是指弧形或扇形所在的椭圆左上角和右下角的坐标。如图所示：

![](./11.png)

startAngle是弧形的起始角度（x轴的正方向，即正右的方向，是0度的位置；顺时针为正角度，逆时针为负角度），sweepAngle是弧度划过的角度；
useCenter表示是否连接到圆心，如果不连接到圆心，就是弧形，如果连接到圆心，就是扇形。
