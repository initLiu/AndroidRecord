默认的Dialog弹框是不阻塞主线程的, 也就是弹框出现之后, UI 线程还会继续在弹框后面执行.
在某些特殊情况我们可能需要等待用户输入或者用户确认才能进行下一步, 就需要一个阻塞住主线程的弹框.
如果用线程的wait等方式, 会造成ANR, 所以这里提供了一个简单有效的实现.
原理见代码注释.

```java
// 这个 handler 用于抛出一个异常, 异常被捕获的时候停止阻塞主线程
final Handler hander = new Handler() {
	@Override
	public void handleMessage(Message mesg) {
		throw new RuntimeException();//抛出异常,在31行catch住，退出loop。
	}
};

AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
builder.setTitle("主线程现在被阻塞了");
builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
	public void onClick(DialogInterface dialog, int which) {
		// 让 handler 抛出异常, 退出对主线程的阻塞
		Message m = hander.obtainMessage();
		hander.sendMessage(m);
	}
});
builder.show();

// 下面是实现阻塞主线程的关键,loop()后面的代码如果执行，只有在退出loop后才会执行。
try {
	Looper.getMainLooper().loop();
}
catch(RuntimeException e2)
{
}
```