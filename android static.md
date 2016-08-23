Lets start with a bit of background: What happens when you start an application?
The OS starts a process and assigns it a unique process id and allocates a process table.A process start an instance of DVM(Dalvik VM); Each application runs inside a DVM.
A DVM manages class loading unloading, instance lifecycle, GC etc.

Lifetime of a static variable: A static variable comes into existence when a class is loaded by the JVM and dies when the class is unloaded.

So if you create an android application and initialize a static variable, it will remain in the JVM until one of the following happens:
1. the class is unloaded
2. the JVM shuts down
3. the process dies

Note that the value of the static variable will persist when you switch to a different activity of another application and none of the above three happens. Should any of the above three happen the static will lose its value.

You can test this with a few lines of code:

1. print the uninitialized static in onCreate of your activity -> should print null
2. initialize the static. print it -> value would be non null
3. Hit the back button and go to home screen. Note: Home screen is another activity.
4. Launch your activity again -> the static variable will be non-null
5. Kill your application process from DDMS(stop button in the devices window).
6. Restart your activity -> the static will have null value.

1.特别留意单例对象中不合理的持有
虽然单例模式简单实用，提供了很多便利性，但是**因为单例的生命周期和应用保持一致**，使用不合理很容易出现持有对象的泄漏。

2.谨慎使用static对象
**因为static的生命周期过长，和应用的进程保持一致**，使用不当很可能导致对象泄漏，在Android中应该谨慎使用static对象。

类的成员变量有两种：一种是被static关键字修饰的变量，叫类变量或静态变量，一种是没有被static修饰的，叫做实例变量 
    静态变量和实例变量的区别在于： 
    类静态变量在内存中只有一个，java虚拟机在加载类的过程中为静态变量分配内存，静态变量位于方法区，被类的所有实例共享，静态变量可以通过类名直接访问。静态变量的生命周期取决于类的生命周期，当类被加载的时候，静态变量被创建并分配内存空间，当类被卸载时，静态变量被摧毁，并释放所占有的内存。 
    类的每一个实例都有相应的实例变量，每创建一个类的实例，java虚拟机为实例变量分配一次内存，实例变量位于堆区中，实例变量的生命周期取决于实例的生命周期，当创建实例时，为实例变量背创建，并分配内存，当实例被销毁时，实例 变量被销毁，并释放所占有的内存空间。 
    假如成员变量时引用变量，该成员变量结束生命周期时，并不意味着它所引用对象也结束生命周期。变量的生命周期和对象的生命周期是不同的概念。