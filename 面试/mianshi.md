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
***
## 静态属性和静态方法是否可以被继承？是否可以被重写？以及原因？
静态属性和静态方法可以被继承
不能被重写。原因：在类加载到内存的时候，静态方法和静态属性就已经被分配了相应的内存空间，
***
## 静态内部类的设计意图
静态内部类增加代码的聚合提高代码的维护性。静态内部类不依赖于外部类