AppBarLayout继承自LinearLayout，只支持竖直方向的布局。  
通过注解的方式给AppBarLayout设置了Behavior
```java
@CoordinatorLayout.DefaultBehavior(AppBarLayout.Behavior.class)
public class AppBarLayout extends LinearLayout {

}
```
## AppBarLayout作用
AppBarLayout的作用是将所有的子View看做一个整体，对这个整体进行滑动处理。

AppBarLayout使用AppBarLayout.Behavior类，处理通过CoordinateLayout传递过来的事件（嵌套滑动相关、touch事件相关、layout相关、状态处理相关），然后调用AppBarLayout.offsetTopAndBottom和offsetLeftAndRight方法移动AppBarLayout。  
移动之后，AppBarLayout会调用OnOffsetChangedListener接口，通知所有的观察者。

