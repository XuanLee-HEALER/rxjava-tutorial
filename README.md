# rxjava-tutorial

关于学习rxJava的demo

## 第二章

### 主要内容

* `Observable`类
* `Observer`接口
* `Observable`工厂
* `Single`、`Completable`、`Maybe`
* `Disposable`

### The `Observable`

对于一个`Observable<T>`，它将T类型的值**推送**一系列操作中，最终被`Observer`消费

### The workings of `Observable`

`Observable`通过三类事件来推送：

* `onNext()` 每次一个将内容推送到`Observer`
* `onComplete()` 一个`Observer`的完成事件，之后不会再发生`onNext()`事件
* `onError()` 发生错误，并定义如何处理错误，如果没有`retry()`操作符，那么会中断链式操作

### 使用`Observable.create()`

`Observable.create()`（工厂方法）创建了一个链式操作的起始点。它可以接收一个`ObservableOnSubscribe`
类型作为参数，这个类型只有一个方法`subscribe(ObservableEmitter emitter)`，这个类型继承了`Emitter`接口，这个接口有上述的三个方法。因为`ObservableOnSubscribe`
只有一个抽象方法（函数式接口），我们可以通过lambda表达式来实现它

 ```java
public class Ch2_1 {

    public static void main(String[] args) {
        Observable<String> source = Observable.create(emitter -> {
            emitter.onNext("Go");
            emitter.onNext("to");
            emitter.onNext("school");
            emitter.onComplete();
        });

        source.subscribe(System.out::println);
    }
}
```

> 推送的值不能并行运算，即每次只能推送一个值。

一个`Observable`可以是无限的。技术上可以终止提供`onNext()`的调用而不提供`onComplete()`的调用，但是这并不是一个好设计

当处理逻辑出现异常，可以由`Observer`来处理。通过重载方法`subscribe(Consumer<String> onNext, Consumer<Throwable> onError)`

```
source.subscribe(System.out::println, Throwable::printStackTrace);
```

emitter中的这些方法并不需要一定将数据推给最终的`Observer`，在链式操作中的其它操作符也可以应用到这些数据上。因为每个`map()`和`filter()
`可以产生新的Observable，通过下一个操作符我们可以链起每个中间变量

> rxJava2.x不支持emit一个null值，可以通过`Optional`来包装值

### 使用`Observable.just()`

我们可以向`just()`方法内传入最多10个要emit的值，会对每个值调用`onNext()`，然后在推送完值之后调用`onComplete()`

我们也可以使用`Observable.fromIterable()`来从`Iterable`类型中emit值，例如`List`，调用过程和`just()`类似

### `Observer`接口

```java
public interface Observer<T> {
    void onSubscribe(@NonNull Disposable d);
    void onNext(@NonNull T value);
    void onError(Throwable e);
    void onComplete();
}
```

`Observable`实现了函数式接口`ObservableSource`，这个接口只有一个方法`subscribe(Observer<T> observer)
`，当传入一个实现了`Observer`接口对象或者一个lambda函数，此时我们将这个`Observer`注册到了`Observable`的emission上

如果不指定`onError()`可能会出现异常进入JVM并导致程序崩溃

### Cold versus hot observables

#### A cold `Observable`

一个冷`Observable`像一张CD，每个人都可以听，它会对每个`Observer`重新分配一个emission，保证它们可以获得所有数据。很多数据驱动的`Observable`都是冷的，包括使用`Observable.just
()`和`Observable.fromIterable()`两个工厂方法创建的

#### A hot `Observable`

一个热`Observable`像一个广播站，它将emission的数据广播到所有的`Observer`，如果一个`Observer`先注册了，那么它将会先消费一些数据，而后面注册的将会丢失这些数据

逻辑上，热`Observable`经常表示事件而不是有限的数据

#### `ConnectableObservable`

这是一个辅助类，可以将任何`Observable`转换为一个热的`Observable`，用法是在普通的`Observable`上调用`publish()
`方法，这种方式下，`subscribe`并不会直接消费数据，需要显式调用`connect()`方法，在调用这个方法之后注册的`Observer`会丢失之前的数据