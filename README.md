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

### 其它的工厂方法

#### `Observable.range()`

参数为`(init, length)`，一次emit一个值，生成整数序列，类似方法还有`rangeLong()`

#### `Observable.interval()`

基于时间的`Observable`，它在指定时间间隔下生成`long`值（无限）。因为实现是基于定时器，所以会在**另外一个线程**下进行计算

这个方法生成的`Observable`是冷的，使用`publish()`方法可以变成热的

#### `Observable.future()`

使用这个方法可以将`future`转换为`Observable`

#### `Observable.empty()`

一个空的`Observable`代表rxJava中null的概念，比较常见的用法是给它提供一个`onComplete()`方法

#### `Observable.never()`

这个`Observable`通常是用来测试用，它不会生成`onComplete()`事件，会让`Observer`等待

#### `Observable.error()`

这个`Observable`基本也只是测试用，它接收一个特定类型的异常对象作为参数，生成`onError()`事件

#### `Observable.defer()`

对于每个`Observer`可以创建不同的状态。即某个`Observable`中应用了几个参数，当参数改变后，`Observer`并不会感知到。通过`defer()`可以传递参数的状态

#### `Observable.fromCallable()`

在`Observable`创建完成之前的异常会按照Java传统方式抛出，而不会传入`onError()
`事件作为参数。使用这个方法可以让参数内容（表达式计算）延迟到`Observable`创建完成之后，这样如果表达式计算过程中出现异常，那么这个异常可以被传递到`onError()`事件中

### `Single`、`Completable`、`Maybe`

接收一个或零个emission的特殊形式的`Observable`

#### `Single`

`Single`实现了`SingleSource`函数式接口，这个接口只有一个方法`subscribe(SingleObserver observer)`

```java
// SingleObserver的接口定义
interface SingleObserver<T> {
    void onSubscribe(@NonNull Disposable d);
    void onSuccess(T value);
    void onError(@NonNull Throwable error);
}
```

`onSuccess()`结合了`onNext()`和`onComplete()`，所以只需要提供一个lambda表达式

`Single`和`Observable`有方法可以互相转换，例如`Observable`的`first()`方法，它接收一个参数作为如果没有元素而使用的默认值。如果确认emission中只有一个值，那么应该使用`Single`

#### `Maybe`

```java
public interface MaybeObserver<T> {
    void onSubscribe(@NonNull Disposable d);
    void onSuccess(T value);
    void onError(@NonNull Throwable e);
    void onComplete();
}
```

`Maybe<T>`会emit一个或者零个值，它会将这个值传入`onSuccess()`，如果没有值则调用`onComplete()`,`Maybe.just()`和`Maybe.empty()
`分别创建一个和零个值的`Observable`

对于`Maybe`来说，要么将值传递给`onSuccess()`事件，要么直接调用`onComplete()`事件

#### `Completable`

```java
interface CompletableObserver<T> {
    void onSubscribe(@NonNull Disposable d);
    void onComplete();
    void onError(@NonNull Throwable error);
}
```

`Completable.fromRunnable()`执行完特定动作后会直接调用`onComplete()`事件，`Completable.complete()`直接调用该事件

### Disposing

在创建`Observable`和`Observer`消费过程中会使用到资源，我们需要提供释放资源的逻辑来完成gc

在有限的`Observable`中可以通过`onComplete()`来完成这项工作

`Disposable`是`Observable`和`Observer`之间的联系，它的`dispose()`方法可以停止emission，还有`isDispose()`方法判断是否已经执行了此操作

被dispose的`Observer`使用的资源都会被释放

`Disposable`作为参数传递仅`onSubscribe()`事件，这使得`Observer`可以在任何时刻丢弃注册的`Observable`

```java
class Test {
    public static void main(String[] args) {
        Observer<Integer> myObserver = new Observer<Integer>() {
            private Disposable disposable;
            @Override
            public void onSubscribe(Disposable disposable) {
                // 初始化过程
                this.disposable = disposable;
            }
            @Override
            public void onNext(Integer value) {
                //has access to Disposable
            }
            @Override
            public void onError(Throwable e) {
                //has access to Disposable
            }
            @Override
            public void onComplete() {
                //has access to Disposable
            }
        };       
    }
}
```

如果不想显式处理`Disposable`，可以通过继承`ResourceObserver`，使用`subscribeWith()
`方法来注册，此时会得到一个默认的`Disposable`。如果提供的是一个完整的`Observer`，那么`subscribe`会返回`void`

#### `CompositeDisposable`

通过`CompositeDisposable`可以统一处理多个`Disposable`

如果使用`Observable.create()`来创建了一个无限的`Observable`，那么在emit值之前可以判断`isDispose()`来决定当前的注册是否有效

`ObservableEmitter`有`setCancellable()`和`setDisposable()`来处理Observable中使用的资源问题，防止资源泄露

## 第三章

### 主要内容

* Conditional operators
* Suppressing operators
* Transforming operators
* Reducing operators
* Boolean operators
* Collection operators
* Error recovery operators
* Action operators
* Utility operators

### 