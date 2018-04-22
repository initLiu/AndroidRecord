```java
Subscriber<Course> subscriber = new Subscriber<Course>() {
    @Override
    public void onCompleted() {

    }

    @Override
    public void onError(Throwable e) {

    }

    @Override
    public void onNext(Course course) {
        Log.e("Test",course.name);
    }
};

Observable.from(students)
        .flatMap(new Func1<Student, Observable<Course>>() {
            @Override
            public Observable<Course> call(Student student) {
                return Observable.from(student.courses);
            }
        })
        .subscribe(subscriber);
```

```java
public final <R> Observable<R> flatMap(Func1<? super T, ? extends Observable<? extends R>> func) {
    if (getClass() == ScalarSynchronousObservable.class) {
        return ((ScalarSynchronousObservable<T>)this).scalarFlatMap(func);
    }
    return merge(map(func));
}

public final <R> Observable<R> map(Func1<? super T, ? extends R> func) {
    return unsafeCreate(new OnSubscribeMap<T, R>(this, func));
}
```
通过查看源码可以看到，调用flatMap方法后，
1. 会先通过map方法创建一个OnSubscribeMap对象，这个OnSubscribeMap对象持有原Observable对象的引用以及Func1对象的引用。  
2. 然后通过unsafeCreate方法创建了一个Observable对象。  
经过这两步之后对象之间的依赖关系如下图：  
![](./7.png)  
接着进行关键的第三步  
3. 调用merge方法
```java
public static <T> Observable<T> merge(Observable<? extends Observable<? extends T>> source) {
    if (source.getClass() == ScalarSynchronousObservable.class) {
        return ((ScalarSynchronousObservable<T>)source).scalarFlatMap((Func1)UtilityFunctions.identity());
    }
    return source.lift(OperatorMerge.<T>instance(false));
}

public final <R> Observable<R> lift(final Operator<? extends R, ? super T> operator) {
    return unsafeCreate(new OnSubscribeLift<T, R>(onSubscribe, operator));
}
```
通过代码可以看到，merge方法对第二步中新生成的Observable对象做了lift操作，根据上一篇的分析，我们可以知道调用lift之后，会创建一个OnSubscribeLift对象，这个对象持有原Observable的onSubscribe对象以及一个新创建的Operator对象，最后调用unsafeCreate方法创建一个Observable对象返回。  

经过这三步之后，对象关系如下图所示：  
![](./8.png)  


之后调用subscribe(Subscribe)方法订阅事件
```java
public final Subscription subscribe(Subscriber<? super T> subscriber) {
    return Observable.subscribe(subscriber, this);
}

static <T> Subscription subscribe(Subscriber<? super T> subscriber, Observable<T> observable) {
    subscriber.onStart();
    ...
    RxJavaHooks.onObservableStart(observable, observable.onSubscribe).call(subscriber);
    ...
}
```
**<font color='red'>注意：这里的observable对象，是在上面的第三步merge方法生成的observable对象，所以这里的observable.onSubscribe对象就是OnSubscribeLift类型</font>**  

从上面代码可以看到最后调用了OnSubscribeLift对象的call方法，参数为调用subscribe方法传入的subscribe对象。
```java
public final class OnSubscribeLift<T, R> implements OnSubscribe<R> {
    final OnSubscribe<T> parent;

    final Operator<? extends R, ? super T> operator;
    public OnSubscribeLift(OnSubscribe<T> parent, Operator<? extends R, ? super T> operator) {
        this.parent = parent;
        this.operator = operator;
    }

    @Override
    public void call(Subscriber<? super R> o) {
        try {
            Subscriber<? super T> st = RxJavaHooks.onObservableLift(operator).call(o);//1.1
            try {
                // new Subscriber created and being subscribed with so 'onStart' it
                st.onStart();
                parent.call(st);//1.1
            } catch (Throwable e) {
                // localized capture of errors rather than it skipping all operators
                // and ending up in the try/catch of the subscribe method which then
                // prevents onErrorResumeNext and other similar approaches to error handling
                Exceptions.throwIfFatal(e);
                st.onError(e);
            }
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            // if the lift function failed all we can do is pass the error to the final Subscriber
            // as we don't have the operator available to us
            o.onError(e);
        }
    }
}

public final class OperatorMerge<T> implements Operator<T, Observable<? extends T>> {
    ...
    @Override
    public Subscriber<Observable<? extends T>> call(final Subscriber<? super T> child) {
        MergeSubscriber<T> subscriber = new MergeSubscriber<T>(child, delayErrors, maxConcurrent);
        MergeProducer<T> producer = new MergeProducer<T>(subscriber);
        subscriber.producer = producer;

        child.add(subscriber);
        child.setProducer(producer);

        return subscriber;
    }
    ...
}
```
**1.1**  
调用OnSubscribeLift对象的call方法后，这里通过operator对象创建了一个MergeSubscriber对象，这个新创建的Subscriber对象持有原Subscriber对象的引用，并且还持有一个producer对象的引用。  

现在对象关系图如下所示：  
![](./9.png)  


**1.2**  
从代码中可以看到，在创建完MergeSubscriber对象后，然后调用了parent.call(st);方法。  
这里的parent对象指向的是OnSubscribeMap对象，st对象指向的是刚才创建MergeSubscriber对象。
```java
public final class OnSubscribeMap<T, R> implements OnSubscribe<R> {

    final Observable<T> source;

    final Func1<? super T, ? extends R> transformer;

    public OnSubscribeMap(Observable<T> source, Func1<? super T, ? extends R> transformer) {
        this.source = source;
        this.transformer = transformer;
    }

    @Override
    public void call(final Subscriber<? super R> o) {
        MapSubscriber<T, R> parent = new MapSubscriber<T, R>(o, transformer);
        o.add(parent);
        source.unsafeSubscribe(parent);
    }
}
```
从代码中可以看到，调用OnSubscribeMap.call方法后，
1. 会创建一个MapSubscriber对象，这个对象持有调用call方法时传入的Subscriber对象的引用以及Func1对象的引用。  

分析到这里，对象关系如下图所示：  
![](./10.png)  


2. 接着调用source.unsafeSubscribe(parent);  
这里的source指向的是原始的Observable，调用unsafeSubscribe方法后，会调用原始Observable的OnSubscribe对象的call方法，传入的是刚才创建的MapSubscriber对象。  
在原始Observable的OnSubscribe对象的call方法中调用了MapSubscriber的onNext(T)方法
```java
static final class MapSubscriber<T, R> extends Subscriber<T> {
    final Subscriber<? super R> actual;

    final Func1<? super T, ? extends R> mapper;

    ....

    @Override
    public void onNext(T t) {
        R result;

        try {
            result = mapper.call(t);//1.3
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            unsubscribe();
            onError(OnErrorThrowable.addValueAsLastCause(ex, t));
            return;
        }

        actual.onNext(result);//1.4
    }
    ...
}
```
**1.3**  
这里的mapper指向的是调用flatMap方法传入的Func1对象，在这个匿名内部类中实现了call方法，返回的是一个Observable对象。所以这里的result是Observable对象。  
**1.4**
接着调用<code><actual.onNext(result);</code>方法。  
从前面的分析可以知道，这里的actual对象指向的是MergeSubscriber对象。
```java
static final class MergeSubscriber<T> extends Subscriber<Observable<? extends T>> {
    ...
    @Override
    public void onNext(Observable<? extends T> t) {
        if (t == null) {
            return;
        }
        if (t == Observable.empty()) {
            emitEmpty();
        } else
        if (t instanceof ScalarSynchronousObservable) {
            tryEmit(((ScalarSynchronousObservable<? extends T>)t).get());
        } else {
            InnerSubscriber<T> inner = new InnerSubscriber<T>(this, uniqueId++);
            addInner(inner);
            t.unsafeSubscribe(inner);
            emit();
        }
    }
}
```
从上面的代码可以看到，
1. 在MergeSubscriber.call方法内部又创建了一个InnerSubscriber对象，这个对象持有MergeSubscriber对象的引用。  

分析到这里，对象关系图如下所示：  
![](./11.png)  

2. 调用<code>t.unsafeSubscribe(inner);</code>方法。  
**<font color='red'>这里的t对象为onNext方法的参数Observable，实际上就是flatMap方法中创建的匿名内部类Func1的call方法返回的Observable对象。</font>**  
unsafeSubscribe(Subscriber)方法会调用Observable中的OnSubscribe对象的call(Subscriber)方法，参数为刚才创建的nnerSubscriber对象。
```java
static final class InnerSubscriber<T> extends Subscriber<T> {
    final MergeSubscriber<T> parent;
    ...
    public InnerSubscriber(MergeSubscriber<T> parent, long id) {
        this.parent = parent;
        this.id = id;
    }
    ...
    @Override
    public void onNext(T t) {
        parent.tryEmit(this, t);
    }
    ...
}

void tryEmit(InnerSubscriber<T> subscriber, T value) {
    boolean success = false;
    long r = producer.get();
    if (r != 0L) {
        synchronized (this) {
            // if nobody is emitting and child has available requests
            r = producer.get();
            if (!emitting && r != 0L) {
                emitting = true;
                success = true;
            }
        }
    }
    if (success) {
        RxRingBuffer subscriberQueue = subscriber.queue;
        if (subscriberQueue == null || subscriberQueue.isEmpty()) {
            emitScalar(subscriber, value, r);
        } else {
            queueScalar(subscriber, value);
            emitLoop();
        }
    } else {
        queueScalar(subscriber, value);
        emit();
    }
}

protected void emitScalar(InnerSubscriber<T> subscriber, T value, long r) {
    ...
    child.onNext(value);
    ....
}
```
从上面的代码可以看到在InnerSubscriber.call方法中调用了MergeSubscriber.tryEmit方法，之后调用目标原始的Subscriber对象的onNext()方法。

通过上面的分析Rxjava在内部通过Decorator模式实现了flatMap的对象变换。
