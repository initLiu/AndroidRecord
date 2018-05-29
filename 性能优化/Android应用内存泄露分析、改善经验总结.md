通过这几天对好几个应用的内存泄露检测和改善，效果明显：

完全退出应用时，手动触发GC，从原来占有内存100多M降到低于20M；

手动触发GC后，通过adb shell dumpsys meminfo packagename -d查看Activity和View的数量也趋近于0了（没有做到归零是因为SDK中存在内存泄露，需要中间层去处理）；

发现了一个SDK中的内存泄露（[Android InputMethodManager 导致的内存泄露及解决方案](https://zhuanlan.zhihu.com/p/20828861)）；

发现一个MTK Webview的内存泄露(org.chromium.android_webview.AwPasswordHandler.java中private static AwPasswordHandler sInstance = null导致的内存泄露)。

从结果来看我分析和改善内存泄露的方法是对的，这个过程并不复杂，所以可以梳理总结出来作为分享。

原则

对于性能问题，分析和改善有必要遵循以下原则：

一切看数据说话，不能跟着感觉走，感觉哪有问题就去改，很有可能会适得其反；

性能优化是一个持续的过程，需要不断地改善，不要想着一气呵成；

对于性能问题，不一定必须要改善，受限于架构或者其它原因某些问题可能会很难改善，必须要先保证能用，再才考虑好用。

改善后一定要验证，任何一个地方的改动都需要验证，避免因为改善性能问题导致其它的问题。

步骤

下面是我在针对内存泄露这个性能问题上的解决步骤：

优先处理常见的内存泄露问题

首先解决常见的内存泄露问题，这个过程可以借助Android Studio的Analyze-Inspect Code对代码做静态分析，常见的内存泄露问题有：

非静态内部类导致的内存泄露，比如Handler，解决方法是将内部类写成静态内部类，在静态内部类中使用软引用/弱引用持有外部类的实例，eg：

      static class ExerciseHandler extends Handler{
          private SoftReference<ExerciseActivity> exerciseActivitySoftReference = null;

          public ExerciseHandler(ExerciseActivity exerciseActivity){
              exerciseActivitySoftReference = new SoftReference<ExerciseActivity>(exerciseActivity);
          }

          @Override
          public void handleMessage(Message msg) {
              ExerciseActivity exerciseActivity = exerciseActivitySoftReference.get();
              if(null != exerciseActivity){
                  super.handleMessage(msg);
                  switch (msg.what) {
                      case MSG_XX:
                          exerciseActivity.***;
                          break；
                      default:
                          break;
                  }
              }
          }
      }
IO操作后，没有关闭文件导致的内存泄露，比如Cursor、FileInputStream、FileOutputStream使用完后没有关闭，这种问题在Android Studio 2.0中能够通过静态代码分析检查出来，直接改善就可以了；

自定义View中使用TypedArray后，没有recycle，这种问题也可以在Android Studio 2.0中能够通过静态代码分析检查出来，直接改善就可以了；

某些地方使用了四大组件的context，在离开这些组件后仍然持有其context导致的内存泄露，这种问题属于共识，在编写代码的过程中就应该按照规则来，使用Application的Context就可以解决这类内存泄露的问题了，至于什么情况下应该使用四大组件的Context，什么时候应该使用Application的context可以参见下表：

备注：大家注意看到有一些NO上添加了一些数字，其实这些从能力上来说是YES，但是为什么说是NO呢？下面一个一个解释：

1、数字1：启动Activity在这些类中是可以的，但是需要创建一个新的task，一般情况不推荐；

2、数字2：在这些类中去layout inflate是合法的，但是会使用系统默认的主题样式，如果你自定义了某些样式可能不会被使用；

3、数字3：在Receiver为null时允许，在4.2或以上的版本中，用于获取黏性广播的当前值。（可以无视）；

4、ContentProvider、BroadcastReceiver之所以在上述表格中，是因为在其内部方法中都有一个context用于使用。

还有一种不属于内存泄露，但在分析内存泄露的问题时应该一并解决：同一个APP，将图片放在不同的drawable文件夹下，在相同的设备上占用的内存情况不一样，具体可以参见：[关于Android中图片大小、内存占用与drawable文件夹关系的研究与分析](http://blog.csdn.net/zhaokaiqiang1992/article/details/49787117)。解决这个问题遵循以下原则就可以了：

1、UI只提供一套高分辨率的图，图片建议放在drawable-xxhdpi文件夹下（放在xxxhdpi或者更高分辨率的文件夹下没有必要，权衡利弊，照顾主流设备即可），这样在低分辨率设备中图片的大小只是压缩，不会存在内存增大的情况；

2、涉及到桌面插件或者不需要缩放的图片，放在drawable-nodpi文件夹下，这个文件夹下的图片在任何设备上都是不会缩放的。

通过工具检查程序运行后的内存泄露

通过上面的步骤，应用中的大部分内存泄露问题都能够得到解决，还有一些内存泄露，需要运行程序，分析运行后的内存快照来解决，比如注册之后没有反注册、类中的静态成员变量导致的内存泄露、SDK中的内存泄露等。解决这类问题可以分两步进行：

通过内存泄露检测工具先定位是哪有问题，内存泄露的检测有两种比较便捷的方式：

1、一种是使用开源项目[Leakcanary](https://github.com/square/leakcanary)，需要添加到代码中，运行后生成分析结果；

2、另一种方式是使用adb shell dumpsys meminfo packagename -d命令，在进入一个界面之前查看一遍Activity和View的数量，在退出这个界面之后再查看一遍Activity和View的数量，对比进入前和进入后Activity和View数量的变化情况，如果有差异，则说明存在内存泄露（在使用命令查看Activity和View的数量之前，记得手动触发GC）。

备注：在Android Studio中，可以通过如下方式获取当前选中进程的内存信息：

然后通过MAT取程序运行时的内存快照做详细分析，对于MAT的使用，网上有很多优质的文章，比如：[Android 性能优化之使用MAT分析内存泄露问题](http://blog.csdn.net/xiaanming/article/details/42396507)，在使用MAT前，有必要知道这几点：

1、 不要指望MAT明确告诉你哪里存在内存泄露，这需要你根据上一步骤首先定位到可能存在内存泄露的类，然后借助MAT确认是否真的存在内存泄露，具体哪个地方存在内存泄露；

2、借助Retained Size分析某一个类及与之相关的实例所消耗的内存，如果这个类的Retained Size比较大，优先分析；

3、检查某个类是否存在内存泄露时，排除其软/弱/虚引用，右键某个类→Merge Shortest Paths to GC Roots→exclude all phantom/weak/soft etc.references。