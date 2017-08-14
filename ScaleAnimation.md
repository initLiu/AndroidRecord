 // 以下四个值的解释只在RELATIVE_TO_SELF正确
 // fromX：起始该view横向所占的长度与自身宽度比值。
 // toX：结束该view横向所占的长度与自身长度比值。
 // fromY：起始该view纵向所占的长度与自身高度比值。
 // toY：结束该view纵向所占的长度与自身长度比值。
 // 以上四种属性值 0.0表示收缩到没有， 1.0表示正常无伸缩， 值小于1.0表示收缩， 值大于1.0表示放大
 
fromXScale,fromYScale，         动画开始前X,Y的缩放，0.0为不显示，  1.0为正常大小 
toXScale，toYScale，          动画最终缩放的倍数， 1.0为正常大小，大于1.0放大  

 // pivotXType：X轴的伸缩模式，可以取值为ABSOLUTE、RELATIVE_TO_SELF、RELATIVE_TO_PARENT。
 // pivotXValue：为动画相对于物件的X坐标的开始位置
 //
 // pivotYType：Y轴的伸缩模式，可以取值为ABSOLUTE、RELATIVE_TO_SELF、RELATIVE_TO_PARENT。
 // pivotYValue：为动画相对于物件的Y坐标的开始位置
 // pivotXValue、pivotYValue 从0%-100%中取值
 // 50%为物件的X或Y方向坐标上的中点位置 ,如果是伸长，则是端点左右（上下）两边同时伸长toX-fromX（toY-fromy）
 // 100%为物体的右端点（下端点），如果是伸长，则只是向左（上）伸长toX-fromX（toY-fromy），端点的另一边无任何动作。
 params2.width = screenWidth;
 view2.setLayoutParams(params2);
 Animation scaleAnimation = new ScaleAnimation(0f, 1f, 1f, 1f,
         Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0);
 scaleAnimation.setRepeatMode(Animation.RESTART);
 scaleAnimation.setRepeatCount(Animation.INFINITE);
 scaleAnimation.setDuration(500);

 view2.startAnimation(scaleAnimation);



 在xml中
 android:pivotX	
 缩放起点X轴坐标（数值、百分数、百分数p，譬如50表示以当前View左上角坐标加50px为初始点、50%表示以当前View的左上角加上当前View宽高的50%做为初始点、50%p表示以当前View的左上角加上父控件宽高的50%做为初始点）