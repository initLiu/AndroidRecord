# 1
1. synchronized关键字可以作为函数的修饰符，也可作为函数内的语句，也就是平时说的同步方法和同步语句块。如果 再细的分类，synchronized可作用于instance变量、object reference（对象引用）、static函数和class literals(类名称字面常量)身上。
2. 无论synchronized关键字加在方法上还是对象上，它取得的锁都是对象，而不是把一段代码或函数当作锁(仅代表同步的声明周期)――而且同步方法很可能还会被其他线程的对象访问。
3. 每个对象只有一个锁（lock）与之相关联
4. 实现同步是要很大的系统开销作为代价的，甚至可能造成死锁，所以尽量避免无谓的同步控制。


# 2
1. 当两个并发线程访问同一个对象object中的这个synchronized(this)同步代码块时，一个时间内只能有一个线程得到执行。另一个线程必须等待当前线程执行完这个代码块以后才能执行该代码块。
2. 然而，当一个线程访问object的一个synchronized(this)同步代码块时，另一个线程仍然可以访问该object中的非synchronized(this)同步代码块。
3. 尤其关键的是，当一个线程访问object的一个synchronized(this)同步代码块时，其他线程对object中所有其它synchronized(this)同步代码块的访问将被阻塞。
4. 第三个例子同样适用其它同步代码块。也就是说，当一个线程访问object的一个synchronized(this)同步代码块时，它就获得了这个object的对象锁。结果，其它线程对该object对象所有同步代码部分的访问都被暂时阻塞。
5. 以上规则对其它对象锁同样适用.
6. 在 Java 中，不光是类实例，每一个类也对应一把锁，这样我们也可将类的静态成员函数声明为 synchronized ，以控制其对类的静态成
7. synchronized(class)和同步的static函数产生的效果是一样的，取得的锁很特别，是当前调用这个方法的对象所属的类.

记得在《Effective Java》一书中看到过将 Foo.class和 P1.getClass()用于作同步锁还不一样，不能用P1.getClass()来达到锁这个Class的

目的。P1指的是由Foo类产生的对象。

可以推断：如果一个类中定义了一个synchronized的static函数A，也定义了一个synchronized 的instance函数B，那么这个类的同一对象Obj

在多线程中分别访问A和B两个方法时，不会构成同步，因为它们的锁都不一样。A方法的锁是Obj这个对象，而B的锁是Obj所属的那个Class。

# 3 测试代码
```Java

public class TestSync {

	public static void main(String[] args) {
		final TestSync test = new TestSync();
		final TestSync test2 = new TestSync();
		Thread thread1 = new Thread(new Runnable() {
			public void run() {
				test.method1();
			}
		},"Thead1");
		
		Thread thread2 = new Thread(new Runnable() {
			public void run() {
				test2.method2();
			}
		},"Thead2");
		
		thread1.start();
		thread2.start();
	}
	
	public void method1(){
		synchronized (TestSync.class) {
			for (int i = 0; i < 5; i++) {
				System.out.println(Thread.currentThread().getName() + " method1");
				try {
					Thread.sleep(500);
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
		}
	}
	
	public void method2(){
		synchronized (TestSync.class) {
			for (int i = 0; i < 5; i++) {
				System.out.println(Thread.currentThread().getName() + " method2");
				try {
					Thread.sleep(500);
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
		}
	}
}

```