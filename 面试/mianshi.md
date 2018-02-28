## java中==和equals和hashCode的区别
1.==是操作符，equals是方法
2.==比较的是两个对象的印象是否相同。equals是object里面的方法，object类里面equals默认实现就是用==进行比较的，所有object里面==和equals结果是一样的。
但是在一些类中如果覆盖了equals方法的话如string，通过equals比较的是两个对象指向的堆内存的值是否相等。
3.hashcode是对象的hash值。
***
## int、char、long各占多少字节数
int4字节，char2字节，long8字节，short2字节，double8字节
***
## int与integer的区别
int基本类型，Integer是int的封装类

通过Integer.valueOf(int i)方法获取Integer时，Integer会缓存-128~127之间的数据，如果不在-128~127之间则new一个Integer
```java
public static Integer valueOf(int i) {
    if (i >= IntegerCache.low && i <= IntegerCache.high)
        return IntegerCache.cache[i + (-IntegerCache.low)];
    return new Integer(i);
}

private static class IntegerCache {
    static final int low = -128;
    static final int high;
    static final Integer cache[];
    static {
        // high value may be configured by property
        int h = 127;
        String integerCacheHighPropValue =
            sun.misc.VM.getSavedProperty("java.lang.Integer.IntegerCache.high");
        if (integerCacheHighPropValue != null) {
            try {
                int i = parseInt(integerCacheHighPropValue);
                i = Math.max(i, 127);
                // Maximum array size is Integer.MAX_VALUE
                h = Math.min(i, Integer.MAX_VALUE - (-low) -1);
            } catch( NumberFormatException nfe) {
                // If the property cannot be parsed into an int, ignore it.
            }
        }
        high = h;

        cache = new Integer[(high - low) + 1];
        int j = low;
        for(int k = 0; k < cache.length; k++)
            cache[k] = new Integer(j++);

        // range [-128, 127] must be interned (JLS7 5.1.7)
        assert IntegerCache.high >= 127;
    }
}
```
Integer a = Integer.valueOf(10);
Integer b = Integer.valueOf(10);
a==b   true
a.equals(b)  true


Integer a = Integer.valueOf(128);
Integer b = Integer.valueOf(128);
a==b   false
a.equals(b)  true
***
## 谈谈对java多态的理解
多态跟继承有关和方法重写有关跟方法重载无关
父类引用指向子类对象，调用方法时会调用子类，而不是父类
***
## String、StringBuffer、StringBuilder区别
String不可变字符串，StringBuffer/StringBuilder可变字符串
StringBuffer是线程安全的（append方法上加了synchronized），StringBuilder非线程安全
***
## 什么是内部类？内部类的作用
1. 内部类：定义在一个类的内部
2. 内部类作用：多重继承，匿名内部类可以方便的实现闭包，静态内部类可以带来更好的代码聚合提供代码的维护性。
3. 内部类分为：成员内部类，局部内部类，匿名内部类，静态内部类
* 非静态内部类可以访问外部类的所有属性，非静态内部类里不能用static的字段和static方法。但是可以有static final字段。
* 静态内部类只能访问外部类的静态属性和方法，可以有静态和非静态方法和静态字段。
* 局部内部类和成员内部类基本一样只是作用域不同。局部内部类只能访问成员方法中的final变量
* 匿名内部类属于局部内部类。匿名内部类没有名字，只能是实例化一次，没有构造方法，不能使静态的
***
## 抽象类和接口区别
1. 抽象类要被子类继承，接口要被子类实现
2. 接口里面只能对方法进行生命，抽象类既可以对方法进行声明也可以实现
3. 抽象类主要是用来抽象类别的，接口主要是用来抽象方法功能的。
4. 抽象类可以有构造方法，接口没有
5. 抽象方法可以有public，protect，default访问修饰符，接口只能是public
**抽象类里可以没有抽象方法。如果一个类里面有抽象方法，那么这个类一定是抽象类**
***
## 抽象类是否可以没有方法和属性？
可以
***
## 抽象类的意义
为子类提供一个公共的类型，封装子类中重复的内容，子类虽然有不同的实现但是定义是一样的
***
## 接口的意义
1. 重要性：在Java语言中， abstract class 和interface 是支持抽象类定义的两种机制。正是由于这两种机制的存在，才赋予了Java强大的 面向对象能力。
2. 简单、规范性：如果一个项目比较庞大，那么就需要一个能理清所有业务的架构师来定义一些主要的接口，这些接口不仅告诉开发人员你需要实现那些业务，而且也将命名规范限制住了（防止一些开发人员随便命名导致别的程序员无法看明白）。
3. 维护、拓展性：比如你要做一个画板程序，其中里面有一个面板类，主要负责绘画功能，然后你就这样定义了这个类。可是在不久将来，你突然发现这个类满足不了你了，然后你又要重新设计这个类，更糟糕是你可能要放弃这个类，那么其他地方可能有引用他，这样修改起来很麻烦。如果你一开始定义一个接口，把绘制功能放在接口里，然后定义类时实现这个接口，然后你只要用这个接口去引用实现它的类就行了，以后要换的话只不过是引用另一个类而已，这样就达到维护、拓展的方便性。
4. 安全、严密性：接口是实现软件松耦合的重要手段，它描叙了系统对外的所有服务，而不涉及任何具体的实现细节。这样就比较安全、严密一些（一般软件服务商考虑的比较多）。
***
## 父类的静态方法能否被子类重写
不能。如果子类有同名的静态方法，子类会隐藏父类的静态方法。
***
## final，finally，finalize的区别
final 用来修饰类/变量/方法。final修饰的类不能被继承，final修饰的方法不能被重写，final修饰的变量不能更改。
finally和try catch一起。表示无论是否发生异常最后都会执行finally
finalize是object中的方法，垃圾回收器将对象回收之前调用此方法来释放资源
***
## 序列化的方式
实现serializable接口或者parcelable接口
***
## serializable和parcelable的区别
serializable会使用反射，在序列化的过程中产生了使用了反射，会产生许多的临时对象，容易触发垃圾回收。parcelable自己实现封装和解封操作不需要反射，效率更好。
实现parcelable接口需要实现，writeToParcel和实现一个内部类CREATOR（createFromParcel和newArray）
***
## 静态属性和静态方法是否可以被继承？是否可以被重写？以及原因？
静态属性和静态方法可以被继承，不能被重写
***
## 静态内部类的设计意图
静态内部类增加代码的聚合提高代码的维护性。静态内部类不依赖于外部类
***
## 哪些情况下的对象会被垃圾回收机制处理掉
### 1.如何确定一个对象是否可以被回收
通过判断对象的引用链是否可达。通过一系列的名为GC Roots的对象作为起始点，从这些节点开始向下搜索，搜索所走过的路径成为引用链，当一个对象到GC Roots没有任何引用链，即从GC Roots到这个对象不可达，则证明此对象是不可用的。
哪些对象可以作为GC Roots？
* 虚拟机栈中的引用的对象
* 方法区中的类静态属性引用的对象
* 方法区的常亮引用的对象
* 本地方法栈中Native方法引用的对象
### 2.垃圾收集算法
1. 标记清除算法  
标记-清除算法分为标记和清除两个阶段。该算法首次按从根集合进行扫描，对存活的对象进行标记，标记完毕后，在扫描整个空间中未被标记的对象并进行回收。
标记-清除算法的主要不足:
    * 效率问题：标记和清除两个过程的效率都不高
    * 空间问题：标记-清除算法不需要进行对象的移动，并且仅对不存货的对象进行处理，因此标记清除之后会产生大量不连续的内存碎片，空间碎片太多可能会导致以后再程序运行过程中需要分配大对象的时候，无法找到足够的连续内存而不得不提前触发另一次垃圾收集动作。
2. 复制算法  
复制算法将可用内存按容量划分为大小相等的两块，每次只是用其中的一块。当这一块的内存用完了，就将还存活着的对象复制到另外一块上面，然后再把已是用过的内存空间一次清理掉。这种算法适用于对象存活率低的场景，比如新生代。
3. 标记整理算法  
复制手机算法在对象存活率较高时就要进行较多的复制操作，效率将会变低。更关键的是，如果不想浪费50%的空间，就需要有额外的空间进行分配担保，以应对被使用的内存中所有对象都100%存活的极端情况，所以在老年代一般不能直接蒜用这种算法。标记整理算法的标记过程类似标记清除算法，但后续步骤不是直接对可回收对象进行清理，而是让所有存活的对象都向一端移动，然后直接清理掉端边界以外的内存，类似于磁盘整理的过程，改垃圾算法适用于对象存活率高的场景（老年代）。
4. 分代收集算法  
分代收集算法是基于这样一个事实：不同的对象的生命周期是不一样的，而不同声明周期的对象位于堆中不同的区域，因此对堆内存不同区域采用不同的策略进行回收可以提高JVM执行效率。java堆内存一般可以分为新生代、老年代、永久代。  
    * **新生代**  
    新生代的目标就是尽可能快速的手机掉那些生命周期短的对象，一般情况下，所有新生成的对象首先都是放在新生代的。新生代内存按照8:1:1的比例分为一个eden区和两个survivor（survivor0，survivor1）区，大部分对象在Eden区中。在进行垃圾回收时，先将eden区存活对象复制到survivor0区，然后清空eden区，当这个survivor0区也满了时，则将eden区和survivor0区存活对象复制到survivor1区，然后清空eden和这个survivor0区，此时survivor0区是空的，然后交换survivor0和survivor1区的角色（即下次垃圾回收时会扫描eden区和survivor1区），即保持survivor0区为空，如此往复。特别的，当survivor1区也不足以存放eden区和survivor区存活对象时，就将存活对象直接存放到年老代。如果老年代也满了，就会触发一次FullGC，也就是新生代、老年代都进行回收。注意，新生代发生的GC也叫做MinorGC，MinorGC发生频率较高，不一定等Eden区满了才触发。  
    * **老年代**  
    老年代存放的都是一些生命周期较长的对象，就像上面所叙述的那样，在新生代中经历了N次垃圾回收后仍然存活的对象就会被放到老年代中。此外，老年代的内存也比新生代大很多，当老年代满时会触发Major GC（Full GC），老年代对象存活时间较长，因此FullGC发生的频率比较低。  
    * **永久代**  
    永久代主要存放静态文件，如Java类、方法等。永久代对垃圾回收没有显著影响，但是有些应用可能动态生成或者调用一些class，例如使用反射、动态代理、CGLib等bytecode框架时，在这种时候需要设置一个比较大的永久代空间来存放这些运行过程中新增的类。
5. 小结  
由于对象进行了分代处理，因此垃圾回收区域、时间也不一样。垃圾回收有两种类型，MinorGC和FullGC。
MinorGC：对新生代进行回收，不会影响到老年代。因为新生代的Java对象大多死亡频繁，所以MinorGC非常频繁，一般这里使用速度快、效率高的算法，使垃圾回收能尽快完成。
FullGC：也叫MajorGC，对整个堆进行回收，包括新生代和老年代。由于FullGC需要对整个堆进行回收，所以比MinorGC要慢，因此应该尽可能减少FullGC的次数，导致FullGC的原因包括：老年代被写满、永久代被写满、和System.gc()被显示调用等。  
<font color='red'>  
绝大多数刚创建的对象会被分配在Eden区，其中的大多数对象很快就会消亡。Eden区是连续的内存空间，因此在其上分配内存极快；
    * 最初一次，当Eden区满的时候，执行Minor GC，将消亡的对象清理掉，并将剩余的对象复制到一个存活区Survivor0（此时，Survivor1是空白的，两个Survivor总有一个是空白的）；
    * 下次Eden区满了，再执行一次Minor GC，将消亡的对象清理掉，将存活的对象复制到Survivor1中，然后清空Eden区；将Survivor0中消亡的对象清理掉，将其中可以晋级的对象晋级到Old区，将存活的对象也复制到Survivor1区，然后清空Survivor0区；
    * 当两个存活区切换了几次（HotSpot虚拟机默认15次，用-XX:MaxTenuringThreshold控制，大于该值进入老年代，但这只是个最大值，并不代表一定是这个值）之后，仍然存活的对象（其实只有一小部分，比如，我们自己定义的对象），将被复制到老年代。
    * 当年老代内存不足时，将执行Major GC，也叫 Full GC  
**如果对象比较大（比如长字符串或大数组），Young空间不足，则大对象会直接分配到老年代上**
</font>  
***
## 静态代理和动态代理的区别，什么场景使用？
### 动态代理原理
系统通过反射的反射创建一个代理类，这个代理类继承proxy类实现了目标接口。持有一个InvocationHandler对象。
调用代理类的方法时，最后都会调用到这个InvocationHandler对象的invoke方法。
生成代理类的大致代码
```java
public final class $Proxy1 extends Proxy implements Subject{
    private InvocationHandler h;
    private $Proxy1(){}
    public $Proxy1(InvocationHandler h){
        this.h = h;
    }
    public int request(int i){
      	// 创建 method 对象
        Method method = Subject.class.getMethod("request", new Class[]{int.class});
      	// 调用了 invoke() 方法
        return (Integer)h.invoke(this, method, new Object[]{new Integer(i)}); 
    }
}
```