# MotionEvent事件
Android中，事件信息通过MotionEvent来展现，可以用event.getAction()或者getActionMasked()来获取事件（两者区别接下来会说明），常用的事件有如下：
* MotionEvent.ACTION_DOWN：第一个触点被按下时触发
* MotionEvent.ACTION_MOVE：有触点移动时触发
* MotionEvent.ACTION_UP：最后一个触点放开时触发
* MotionEvent.ACTION_CANCEL：当前手势被取消。一般来说，如果一个子视图接收了父视图分发给它的ACTION_DOWN事件，那么与ACTION_DOWN事件相关的事件流就要分发给这个子视图，但是如果父视图希望拦截其中的一些事件，不再继续转发事件给着饿子视图的话，那么就需要给子视图一个ACTION_CANCEL事件
* MotionEvent.ACTION_POINTER_DOWN：多个触点时，按下非第一个点是触发
* MotionEvent.ACTION_POINTER_UP：多个触点时，松开非最后一个点时出发

## pointerId、pointerIndex
MotionEvent中的Pointer表示多点触控时的触点（如手指），一个MotionEvent对象会存储当前所有触点（Pointer）的信息，即使多个触点只有一个移动。  

并且每个pointer都有唯一的pointerId，在此pointer从down到up之间一直是不变的，注意当此pointer失效后，它的pointerId会被新来的pointer使用。  
每个pointer也有一个pointerIndex，其值范围【0，getPointerCount()】，但不像pointerId，pointerIndex是会变化的。比如有两个pointer，他们的index分别为0、1，当index为0的pointer失效后，剩余pointer的index就会变成1.  

MotionEvent类中的很多方法都是可以传入一个int值作为参数的，其实传入的就是pinterIndex。比如
getX(pointerIndex)和getY(pointerIndex)，此时，它们返回的就是pointerIndex所代表的触摸点相关事件坐标值。pointer的id在整个事件流中是不会发生变化的，但是pointerIndex会发生变化。所以，要记录一个触点的事件流时，就需要保存其id，然后使用findPointerIndex(int)来获得其index值，然后再获得其他信息。  
MotionEvent提供的关于pointerId、pointerIndex常用方法：
```java
int getPointerId(int pointerIndex) //通过pointerIndex获取pointerId
float getX(int pointerIndex)
float getY(int pointerIndex)
int findPointerIndex(int pointerId) //通过pointerId获取pointerIndex
```
因此，我们追踪手指的动作时间不可依赖pointerIndex，只能靠pointerId。

## Action、ActionMask、ActionIndex
getActionMasked()、getAction()
MotionEvent只包含一个触点的事件时，上边两个函数的结果是相同的，但是包含多个触点时，返回值就会有差别了。
getAction()值包含pointerIndex值和事件类型ACTION_DOWN、ACTION_UP、ACTION_POINTER_DOWN之类：前8位代表pointerIndex，后8位代表事件类型。

MotionEvent中ACTION的掩码：
public static final int ACTION_MASK = 0xff; //用于获取低8位
public static final int ACTION_POINTER_INDEX_MASK = 0xff00; //用于获取低2字节中的高8位
转化为二进制就是：
ACTION_MASK = 0000000011111111
ACTION_POINTER_INDEX_MASK = 1111111100000000

假设我们操作时getAction()返回0x0105，转化为二进制就是
getAction() = 0000000100000101

这时，
int indexOriginal = getAction() & ACTION_POINTER_INDEX_MASK = 0000000100000000；
int index = indexOriginal >> 8 ;//得到pointerIndex：00000001
int action = getAction() & ACTION_MASK = 0000000000000101 ;//得到pointer的真正action 5，即ACTION_POINTER_DOWN。
而getActionMasked（）就会直接返回5，和以上getAction() & ACTION_MASK运算后结果一样。

对于ACTION_DOWN、ACTION_UP之间的其他点(包括ACTION_POINTER_DOWN、ACTION_MOVE、ACTION_POINTER_UP)，Android称之为maskedAction，可以使用函数public final int getActionMasked()来查询这个动作是ACTION_POINTER_DOWN、ACTION_POINTER_UP还是ACTION_MOVE。

## offsetLocation
MotionEvent提供了getX()和getY()来获取相对于此View的左、上点的坐标，子view的MotionEvent是由父view传过来，在父view里getX()时如果传递给子view不进行处理的话，那么在子view里getX()时会和父view一样，这样获取的值是不对的。所以在ViewGroup分发事件时dispatchTransformedTouchEvent方法有如下处理
```java
final float offsetX = mScrollX - child.mLeft;
final float offsetY = mScrollY - child.mTop;
event.offsetLocation(offsetX, offsetY);//偏移位置，保证子view getX()、getY()正确
handled = child.dispatchTouchEvent(event);//保证传递给子View时，event的getX()、getY() 是相对于该子View的坐标系的坐标值。
event.offsetLocation(-offsetX, -offsetY);//恢复上面第3行的偏移
```
这段代码展示了父视图把事件分发给子视图时，getX()和getY()所获得的相关坐标是如何改变的。当父视图处理事件时，上述两个函数获得的相对坐标是相对于父视图的，当需要将该事件分发给子视图时，就通过上边这段代码，调整了相对坐标的值，让其变为相对于子视图。