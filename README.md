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

`Observable.create()`（工厂方法）创建了一个链式操作的起始点。它可以接收一个`ObservableOnSubscribe`类型作为参数，这个类型只有一个方法`subscribe(ObservableEmitter
 emitter)`，这个类型继承了`Emitter`接口，这个接口有上述的三个方法。因为`ObservableOnSubscribe`只有一个抽象方法（函数式接口），我们可以通过lambda表达式来实现它
 
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

