缩放画布
```java
/**
 *sx,sy x轴和y轴的缩放比例，=1不变，>1放大，<1缩小
 *px,py,缩放的中心点
 **/
canvas.scale(float sx, float sy, float px, float py)
```

```java
Matrix.postScale (float sx, float sy, float px, float py)//参数的意思和canval.scale一样
```
