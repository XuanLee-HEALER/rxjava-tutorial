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

### Conditional operators

条件操作符可以有条件emit值或者传递`Observable`

#### takeWhile()和skipWhile()

`take()`的两个变种，前者只取符合条件的值，后者略过符合条件的值

`takeUntil()`和`takeWhile()`差不多，它接收一个`Observable`作为参数，当这个参数emit值的时候它才会开始emit值。`skipUntil()`是同样的行为

#### defaultIfEmpty()

当我们在确认`Observable`里没有值的时候，仍然希望emit一个默认值，可以使用这个方法

#### switchIfEmpty()

和`defaultIfEmpty()`类似，`switchIfEmpty()`接收一个`Observable`作为参数，如果本身没有值可以emit，那么会将参数作为另一个源。如果前一个源中有值，那么这个方法中的`Observable
`不会emit值

### Suppressing operators

当有一些值不满足指定的规则是，可以通过这些操作符来删除，它们的实现原理是不对这些不满足的值调用`onNext()`方法

#### filter()

`filter()`接收一个`Predicate<T>`，当每个emit的值被计算为`boolean`值时，如果值为false，那么不会进入下游。如果没有满足的值，那么返回的`Observable`是空的

#### take()

`take()`有两个重载版本，一个接收指定数量的emission，然后在值到达后调用`onComplete()`，另一个`take()`接收指定时间段内emit的值。对应的还有`takeLast()`，它会接收`onComplete
()`事件调用前指定数量的值

take()
The take() operator has two overloaded versions. One takes the specified number of
emissions and calls onComplete() after all of them reach it. It will also dispose of the
entire subscription so that no more emissions will occur. For instance, take(2) will emit
the first two emissions and then call onComplete() (this will generate an onComplete
event):
import io.reactivex.rxjava3.core.Observable;
public class Ch3_06 {
public static void main(String[] args) {
Observable.just("Alpha", "Beta", "Gamma")
.take(2)
.subscribe(s -> System.out.println("RECEIVED: " + s));
}
}
The output of the preceding code snippet is as follows:
RECEIVED: Alpha
RECEIVED: Beta
Note that if the take() operator receives fewer emissions than specified, it will simply emit
what it does get and then emit the onComplete event.
The other version of the take() operator accepts emissions within the specific time
duration and then emits onComplete. Of course, our cold Observable emits so quickly
that it would serve as a bad example for this case. Maybe a better example would be to use
an Observable.interval() function.
Let's emit every 300 milliseconds, but set the take() operator to accept emissions for only
2 seconds in the following code snippet:
import io.reactivex.rxjava3.core.Observable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
Basic Operators
Chapter 3
[ 69 ]
import java.util.concurrent.TimeUnit;
public class Ch3_07 {
public static void main(String[] args) {
DateTimeFormatter f = DateTimeFormatter.ofPattern("ss:SSS");
System.out.println(LocalDateTime.now().format(f));
Observable.interval(300, TimeUnit.MILLISECONDS)
.take(2, TimeUnit.SECONDS)
.subscribe(i -> System.out.println(LocalDateTime.now()
.format(f) + " RECEIVED: " + i));
sleep(5000);
}
}
The output of the preceding code is as follows:
50:644
51:047 RECEIVED: 0
51:346 RECEIVED: 1
51:647 RECEIVED: 2
51:947 RECEIVED: 3
52:250 RECEIVED: 4
52:551 RECEIVED: 5
You will likely get output similar to that shown here (with each print happening every 300
milliseconds). The first column is the current time in seconds and milliseconds. As you can
see, we can get only 6 emissions in 2 seconds if they are spaced out by 300 milliseconds
because the first value is emitted after 300 milliseconds too.
Note that there is also a takeLast() operator, which takes the last specified number of
emissions (or time duration) before the onComplete event is generated. Just keep in mind
that it internally queues emissions until its onComplete() function is called, and then it
can identify and emit the last emissions.
skip()
The skip() operator does the opposite of the take() operator. It ignores the specified
number of emissions and then emits the ones that follow. Let's skip the first 90 emissions in
the following code snippet:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_08 {
public static void main(String[] args) {
Observable.range(1, 100)
Basic Operators
Chapter 3
[ 70 ]
.skip(90)
.subscribe(i -> System.out.println("RECEIVED: " + i));
}
}
The output of the following code snippet is as follows:
RECEIVED: 91
RECEIVED: 92
RECEIVED: 93
RECEIVED: 94
RECEIVED: 95
RECEIVED: 96
RECEIVED: 97
RECEIVED: 98
RECEIVED: 99
RECEIVED: 100
Just as in the case of the take() operator, there is also an overloaded version that accepts a
time duration.
And there is a skipLast() operator, which skips the last specified number of items (or
time duration) before the onComplete event is generated. Just keep in mind that the
skipLast() operator queues and delays emissions until it identifies the last specified
number of emissions in that scope.
distinct()
The distinct() operator emits unique emissions. It suppresses any duplicates that follow.
Equality is based on the hashCode() and equals() methods implemented by the emitted
objects. If we want to emit the distinct lengths of strings, this could be done as follows:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_09 {
public static void main(String[] args) {
Observable.just("Alpha", "Beta", "Gamma")
.map(String::length)
.distinct()
.subscribe(i -> System.out.println("RECEIVED: " + i));
}
}
Basic Operators
Chapter 3
[ 71 ]
The output of the preceding code snippet is as follows:
RECEIVED: 5
RECEIVED: 4
Keep in mind that if you have a wide, diverse spectrum of unique values, distinct() can
use a bit of memory. Imagine that each subscription results in a HashSet that tracks
previously captured unique values.
There is an overloaded version of distinct(Function<T,K> keySelector) that accepts
a function that maps each emission to a key used for equality logic. Then, the uniqueness of
each emitted item is based on the uniqueness of this generated key, not the item itself. For
instance, we can use string length as the key used for uniqueness:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_10 {
public static void main(String[] args) {
Observable.just("Alpha", "Beta", "Gamma")
.distinct(String::length)
.subscribe(i -> System.out.println("RECEIVED: " + i));
}
}
The output of the preceding code snippet is as follows:
RECEIVED: Alpha
RECEIVED: Beta
Alpha is five characters, and Beta is four. Gamma was ignored because Alpha was already
emitted as a 5-character length value.
If the generated key is an object, then its uniqueness is based on the equals() method
implemented by that object.
distinctUntilChanged()
The distinctUntilChanged() function ignores consecutive duplicate emissions. If the
same value is being emitted repeatedly, all the duplicates are ignored until a new value is
emitted. Duplicates of the next value will be ignored until it changes again, and so on.
Observe the output for the following code to see this behavior in action:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_11 {
Basic Operators
Chapter 3
[ 72 ]
public static void main(String[] args) {
Observable.just(1, 1, 1, 2, 2, 3, 3, 2, 1, 1)
.distinctUntilChanged()
.subscribe(i -> System.out.println("RECEIVED: " + i));
}
}
The output of the preceding code snippet is as follows:
RECEIVED: 1
RECEIVED: 2
RECEIVED: 3
RECEIVED: 2
RECEIVED: 1
The first emission of 1 gets through to subscribe(). But the next two 1 values are ignored
because they are consecutive duplicates. When the item switches to 2, the first 2 is emitted,
but the following duplicate is ignored. A 3 is emitted and its following duplicate is ignored
as well. Finally, we switch back to a 2, which emits, and then a 1 whose duplicate is
ignored.
Just like with distinct(), you can use distinctUntilChanged() with an optional
argument – a lambda expression for a key generation. In the following code snippet, we
execute the distinctUntilChanged() operation with strings keyed on their lengths:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_12 {
public static void main(String[] args) {
Observable.just("Alpha", "Beta", "Zeta", "Eta", "Gamma", "Delta")
.distinctUntilChanged(String::length)
.subscribe(i -> System.out.println("RECEIVED: " + i));
}
}
The output of the preceding code snippet is as follows:
RECEIVED: Alpha
RECEIVED: Beta
RECEIVED: Eta
RECEIVED: Gamma
Basic Operators
Chapter 3
[ 73 ]
The Zeta value was skipped because it comes right after Beta, which is also four
characters. The Delta value is ignored as well because it follows Gamma, which is five
characters too.
elementAt()
You can get a specific emission by its index specified by the long value, starting at 0. After
the item is found and emitted, onComplete() is called and the subscription is disposed of.
For example, if you want to get the fourth emission coming from an Observable, you can
do it as shown in the following code snippet:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_13 {
public static void main(String[] args) {
Observable.just("Alpha", "Beta", "Zeta", "Eta", "Gamma")
.elementAt(3)
.subscribe(i -> System.out.println("RECEIVED: " + i));
}
}
The output of the following code snippet is as follows:
RECEIVED: Eta
You may not have noticed, but elementAt() returns Maybe<T> instead of
Observable<T>. This is because it yields one emission, but if there are fewer emissions
than the index sought, it will be empty.
There are other flavors of elementAt(), such as elementAtOrError(), which returns a
Single and emits an error if an element at that index is not found. singleElement()
turns an Observable into a Maybe, but produces an error if there is more than one element.
Finally, firstElement() and lastElement() emit the first and the last items,
respectively.
Basic Operators
Chapter 3
[ 74 ]
Transforming operators
In this section, we'll cover operators that transform emissions. You have already seen
map(), which is the most obvious operator in this category. We'll start with that one.
map()
For a given Observable<T>, the map() operator transforms an emitted value of the T type
into a value of the R type (that may or may not be the same type T) using the
Function<T,R> lambda expression provided. We have already used this operator many
times, turning String objects into integers (their lengths), for example. This time, we will
take raw date strings and use the map() operator to turn each of them into a LocalDate
emission, as shown in the following code snippet:
import io.reactivex.rxjava3.core.Observable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
public class Ch3_14 {
public static void main(String[] args) {
DateTimeFormatter dtf = DateTimeFormatter.ofPattern("M/d/yyyy");
Observable.just("1/3/2016", "5/9/2016", "10/12/2016")
.map(s -> LocalDate.parse(s, dtf))
.subscribe(i -> System.out.println("RECEIVED: " + i));
}
}
The output of the preceding code snippet is as follows:
RECEIVED: 2016-01-03
RECEIVED: 2016-05-09
RECEIVED: 2016-10-12
We provided the map() operator with a function (in the lambda expression form) that turns
each String object into a LocalDate object. The DateTimeFormatter format was created
in advance in order to assist with the LocalDate.parse() processing. Finally, we pushed
each LocalDate emission into the Observer to be printed.
The map() operator does a one-to-one conversion of each emitted value. If you need to do a
one-to-many conversion (turn one emission into several emissions), you can use
flatMap() or concatMap(), which we will cover in the next chapter.
Basic Operators
Chapter 3
[ 75 ]
cast()
cast() is a simple, map-like operator that casts each emitted item to another type. If we
need to cast each value emitted by Observable<String> to an Object (and return an
Observable<Object>), we could use the map() operator as shown in the following
example:
Observable<Object> items = Observable.just("Alpha", "Beta", "Gamma")
.map(s -> (Object) s);
Instead, we can use the more specialized shorthand cast(), and simply pass the class type
we want to cast to, as shown in this code snippet:
Observable<Object> items = Observable.just("Alpha", "Beta", "Gamma")
.cast(Object.class);
If you find that you are having typing issues due to inherited or polymorphic types being
mixed, this is an effective brute-force way to cast everything down to a common base type,
but strive to use generics properly and type wildcards appropriately first.
startWithItem()
For a given Observable<T>, the startWithItem() operator (previously called
startWith() in RxJava 2.x) allows you to insert a value of type T that will be emitted
before all the other values. For instance, if we have an Observable<String> that emits
drink names we would like to print, we can use startWithItem() to insert a header as the
first value of the stream:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_15 {
public static void main(String[] args) {
Observable<String> menu =
Observable.just("Coffee", "Tea", "Espresso", "Latte");
//print menu
menu.startWithItem("COFFEE SHOP MENU")
.subscribe(System.out::println);
}
}
Basic Operators
Chapter 3
[ 76 ]
The output of the preceding code snippet is as follows:
COFFEE SHOP MENU
Coffee
Tea
Espresso
Latte
If you want to start with more than one value emitted first, use startWithArray(), which
accepts varargs (an array or any number of String values as parameters). If you need to
add a divider between the header and menu items, start with both the header and divider
as the values passed into the startWithArray() operator, as shown in the following
example:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_16 {
public static void main(String[] args) {
Observable<String> menu =
Observable.just("Coffee", "Tea", "Espresso", "Latte");
//print menu
menu.startWithArray("COFFEE SHOP MENU", "----------------")
.subscribe(System.out::println);
}
}
The output of the preceding code snippet is as follows:
COFFEE SHOP MENU
----------------
Coffee
Tea
Espresso
Latte
The same result can be achieved using startWithIterable(), which accepts an n object
of the iterable type. Here is an example:
List<String> list =
Arrays.asList("COFFEE SHOP MENU", "----------------");
menu.startWithIterable(list).subscribe(System.out::println);
The startWithItem() operator is helpful for cases like this, where we want to seed an
initial value or precede our emissions with one particular value. When more than one value
has to be emitted first, before the values from the source Observable start flowing, the
startWithArray() or startWithIterable() operator is your friend.
Basic Operators
Chapter 3
[ 77 ]
If you want emissions of one Observable to precede the emissions of
another Observable, use Observable.concat() or concatWith(),
which we will cover in the next chapter.
sorted()
If you have a finite Observable<T> that emits items that are of a primitive type, String
type, or objects that implement Comparable<T>, you can use sorted() to sort the
emissions. Internally, it collects all the emissions and then re-emits them in the specified
order. In the following code snippet, we sort items coming from Observable<Integer> so
that they are emitted in their natural order:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_17 {
public static void main(String[] args) {
Observable.just(6, 2, 5, 7, 1, 4, 9, 8, 3)
.sorted()
.subscribe(System.out::print);
}
}
The output of the preceding code snippet is as follows (note that, in order to make the
output more compact, we use print() in this example, instead of println(), which we
have used hitherto):
123456789
Of course, this can have some performance implications and consumes the memory as it
collects all emitted values in memory before emitting them again. If you use this against an
infinite Observable, you may even get an OutOfMemoryError exception.
The overloaded version, sorted(Comparator<T> sortFunction), can be used to
establish an order other than the natural sort order of the emitted items that are of a
primitive type, String type, or objects that implement Comparable<T>. For example, we
can provide Comparator<T> to reverse the sorting order, as in the following code snippet:
import io.reactivex.rxjava3.core.Observable;
import java.util.Comparator;
public class Ch3_18 {
public static void main(String[] args) {
Observable.just(6, 2, 5, 7, 1, 4, 9, 8, 3)
Basic Operators
Chapter 3
[ 78 ]
.sorted(Comparator.reverseOrder())
.subscribe(System.out::print);
}
}
The output of the preceding code snippet is as follows:
987654321
This overloaded version, sorted(Comparator<T> sortFunction), can also be used to
sort the emitted items that are objects that do not implement Comparable<T>.
Since Comparator is a single abstract method interface, you can implement it quickly with
a lambda expression. Specify the two parameters representing two emissions, T o1 and T
o2, and then implement the Comparator<T> functional interface by providing the body for
its compare(T o1, T o2) method. For instance, we can sort the emitted items not
according to their implementation of the compareTo(T o) method (that is,
the Comparable<T> interface), but using the comparator provided. For example, we can
sort String type items not according to their implementation of the Comparable<T>
interface, but according to their length:
import io.reactivex.rxjava3.core.Observable;
import java.util.Comparator;
public class Ch3_19 {
public static void main(String[] args) {
Observable.just("Alpha", "Beta", "Gamma")
.sorted(Comparator.comparingInt(String::length))
.subscribe(System.out::println);
}
}
The output of the preceding code snippet is as follows:
Beta
Alpha
Gamma
Please be aware that the behavior of sorted(Comparator<T> sortFunction) in this
case is the same as the behavior of the following combination of operators:
map(String::length).sorted()
Basic Operators
Chapter 3
[ 79 ]
scan()
The scan() method is a rolling aggregator. It adds every emitted item to the provided
accumulator and emits each incremental accumulated value. For instance, let's emit the
rolling sum of all of the values emitted so far, including the current one, as follows:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_20 {
public static void main(String[] args) {
Observable.just(5, 3, 7)
.scan((accumulator, i) -> accumulator + i)
.subscribe(s -> System.out.println("Received: " + s));
}
}
The output of the preceding code snippet is as follows:
Received: 5
Received: 8
Received: 15
As you can see, first, the scan() operator emitted the value of 5, which was the first value
it received. Then, it received 3 and added it to 5, emitting 8. After that, 7 was received,
which was added to 8, thereby emitting 15.
This operator does not have to be used just for rolling sums. You can create many kinds of
accumulators, even non-math ones such as String concatenations or boolean reductions.
Note that scan() is very similar to reduce(), which we will learn about shortly. Be careful
not to confuse them though. The scan() operator emits the rolling accumulation for each
emission, whereas reduce() yields a single result reflecting the final accumulated value
after onComplete() is called. This means that reduce() has to be used with a finite
Observable only, while the scan() operator can be used with an infinite Observable too.
You can also provide an initial value for the first argument and aggregate the emitted
values into a different type than what is being emitted. If we wanted to emit the rolling
count of emissions, we could provide an initial value of 0 and just add 1 to it for every
emitted value. Keep in mind that the initial value would be emitted first, so use skip(1)
after scan() if you do not want that initial emission to be included in the accumulator:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_21 {
public static void main(String[] args) {
Basic Operators
Chapter 3
[ 80 ]
Observable.just("Alpha", "Beta", "Gamma")
.scan(0, (total, next) -> total + 1)
.subscribe(s -> System.out.println("Received: " + s));
}
}
The output of the preceding code snippet is as follows:
Received: 0
Received: 1
Received: 2
Received: 3
As you can see, the scan() operator emitted 0 first, and then added 1 every time it
received another emission, acting effectively as a counter of the received values.
Reducing operators
You will likely have moments when you need to take a series of emitted values and
aggregate them into a single value (usually emitted through a Single). We will cover a few
operators that accomplish this. Note that nearly all of these operators only work on a finite
Observable that calls onComplete() because, typically, we can aggregate only finite
datasets. We will explore this behavior as we cover these operators.
count()
The count() operator counts the number of emitted items and emits the result through a
Single once onComplete() is called. Here is an example:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_22 {
public static void main(String[] args) {
Observable.just("Alpha", "Beta", "Gamma")
.count()
.subscribe(s -> System.out.println("Received: " + s));
}
}
The output of the preceding code snippet is as follows:
Received: 3
Basic Operators
Chapter 3
[ 81 ]
Like most reduction operators, this should not be used on an infinite Observable. It will
hang up and work indefinitely, never emitting a count or calling onComplete(). If you
need to count emissions of an infinite Observable, consider using scan() to emit a rolling
count instead.
reduce()
The reduce() operator is syntactically identical to scan(), but it only emits the final result
when the source Observable calls onComplete(). Depending on which overloaded
version is used, it can yield Single or Maybe. If you need the reduce() operator to emit
the sum of all emitted integer values, for example, you can take each one and add it to the
rolling total. But it will only emit once—after the last emitted value is processed (and the
onComplete event is emitted):
import io.reactivex.rxjava3.core.Observable;
public class Ch3_23 {
public static void main(String[] args) {
Observable.just(5, 3, 7)
.reduce((total, i) -> total + i)
.subscribe(s -> System.out.println("Received: " + s));
}
}
The output of the preceding code snippet is as follows:
Received: 15
Similar to scan(), there is a seed argument that you can provide that will serve as the
initial value to accumulate on. If we wanted to turn our emissions into a single comma-
separated String value, we could use reduce(), too, as shown in the following example:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_24 {
public static void main(String[] args) {
Observable.just(5, 3, 7)
.reduce("", (total, i) ->
total + (total.equals("") ? "" : ",") + i)
.subscribe(s -> System.out.println("Received: " + s));
}
}
Basic Operators
Chapter 3
[ 82 ]
The output of the preceding code snippet is as follows:
Received: 5,3,7
We provided an empty string as our seed value, and we maintained a rolling concatenation
and kept adding to it. We also prevented a preceding comma using a ternary operator to
check whether the total is the seed value, returning an empty string instead of a comma if
it is.
Your seed value for the reduce() operator should be immutable, such as an integer or
String. Bad side effects can happen if it is mutable. In such cases, you should use
collect() (or seedWith()), which we will cover in a moment.
If you want to reduce the emitted values of type T into a collection, such
as List<T>, use collect() instead of reduce(). Using reduce() will
have the undesired side effect of using the same list for each subscription,
rather than creating a fresh empty one each time.
Boolean operators
There is a sub-category of reducing operators that evaluate the result to a boolean value
and return a Single<Boolean> object.
all()
The all() operator verifies that all emissions meet the specified criterion and returns a
Single<Boolean> object. If they all pass, it returns the Single<Boolean> object that
contains true. If it encounters one value that fails the criterion, it immediately calls
onComplete() and returns the object that contains false. In the following code snippet,
we test six (or fewer) integers, verifying that they all are less than 10:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_25 {
public static void main(String[] args) {
Observable.just(5, 3, 7, 11, 2, 14)
.all(i -> i < 10)
.subscribe(s -> System.out.println("Received: " + s));
}
}
Basic Operators
Chapter 3
[ 83 ]
The output of the preceding code snippet is as follows:
Received: false
When the all() operator encountered 11, it immediately emitted false and called
onComplete(). It did not even receive 2 or 14 because that would be unnecessary work. It
has already found an element that fails the test.
If you call all() on an empty Observable, it will emit true due to the
principle of vacuous truth. You can read more about vacuous truth on
Wikipedia at https:/​/​en.​wikipedia.​org/​wiki/​Vacuous_​truth.
any()
The any() method checks whether at least one emission meets a specified criterion and
returns a Single<Boolean>. The moment it finds an emission that does, it returns
a Single<Boolean> object with true and then calls onComplete(). If it processes all
emissions and finds that none of them meet the criterion, it returns a Single<Boolean>
object with false and calls onComplete().
In the following code snippet, we emit four date strings, convert them into the LocalDate
type, and check whether any are in the month of June or later:
import io.reactivex.rxjava3.core.Observable;
import java.time.LocalDate;
public class Ch3_26 {
public static void main(String[] args) {
Observable.just("2016-01-01", "2016-05-02",
"2016-09-12", "2016-04-03")
.map(LocalDate::parse)
.any(dt -> dt.getMonthValue() >= 6)
.subscribe(s -> System.out.println("Received: " + s));
}
}
The output of the preceding code snippet is as follows:
Received: true
When it encountered the 2016-09-12 date, it immediately emitted true and called
onComplete(). It did not proceed to process 2016-04-03.
Basic Operators
Chapter 3
[ 84 ]
If you call any() on an empty Observable, it will emit false due to the
principle of vacuous truth. You can read more about vacuous truth on
Wikipedia at https:/​/​en.​wikipedia.​org/​wiki/​Vacuous_​truth.
isEmpty()
The isEmpty() operator checks whether an Observable is going to emit more items. It
returns a Single<Boolean> with true if the Observable does not emit items anymore.
In the following code snippet, an Observable emits strings, and neither contain the letter
z. The following filter, however, only allows a downstream flow of those items that do
contain the letter z. This means that, after the filter, the Observable emits no items
(becomes empty), but if the letter z is found in any of the emitted strings, the received
result changes to false, as demonstrated in the following example:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_27 {
public static void main(String[] args) {
Observable.just("One", "Two", "Three")
.filter(s -> s.contains("z"))
.isEmpty()
.subscribe(s -> System.out.println("Received1: " + s));
Observable.just("One", "Twoz", "Three")
.filter(s -> s.contains("z"))
.isEmpty()
.subscribe(s -> System.out.println("Received2: " + s));
}
}
The output of the preceding code snippet is as follows:
Received1: true
Received2: false
contains()
The contains() operator checks whether a specified item (based on the
hashCode()/equals() implementation) has been emitted by the source Observable. It
returns a Single<Boolean> with true if the specified item was emitted, and false if it
was not.
Basic Operators
Chapter 3
[ 85 ]
In the following code snippet, we emit the integers 1 through 10000, and we check whether
the number 9563 is emitted from it using contains():
import io.reactivex.rxjava3.core.Observable;
public class Ch3_28 {
public static void main(String[] args) {
Observable.range(1, 10000)
.contains(9563)
.subscribe(s -> System.out.println("Received: " + s));
}
}
The output of the preceding code snippet is as follows:
Received: true
As you have probably guessed, the moment the specified value is found, the operator
returns Single<Boolean> with true, calls onComplete(), and disposes of the processing
pipeline. If the source calls onComplete() and the element was not found, it returns
Single<Boolean> with false.
sequenceEqual()
The sequenceEqual() operator checks whether two observables emit the same values in
the same order. It returns a Single<Boolean> with true if the emitted sequences are the
same pairwise.
In the following code snippet, we create and then compare observables that emit the same
sequence or different (by order or by value) sequences:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_29 {
public static void main(String[] args) {
Observable<String> obs1 = Observable.just("One","Two","Three");
Observable<String> obs2 = Observable.just("One","Two","Three");
Observable<String> obs3 = Observable.just("Two","One","Three");
Observable<String> obs4 = Observable.just("One","Two");
Observable.sequenceEqual(obs1, obs2)
.subscribe(s -> System.out.println("Received: " + s));
Observable.sequenceEqual(obs1, obs3)
.subscribe(s -> System.out.println("Received: " + s));
Basic Operators
Chapter 3
[ 86 ]
Observable.sequenceEqual(obs1, obs4)
.subscribe(s -> System.out.println("Received: " + s));
}
}
The output of the preceding code snippet is as follows:
Received: true
Received: false
Received: false
As you can see, the output confirms that the sequence of the values emitted by the
observables obs1 and obs2 are equal in size, values, and their order. The observables obs1
and obs3 emit sequences of the same values but in a different order, while the observables
obs1 and obs4 have different sizes.
Collection operators
A collection operator accumulates all emissions into a collection such as a List or Map and
then returns that entire collection as a single value. It is another form of a reducing operator
since it aggregates emitted items into a single one. We will dedicate a section to each of the
collection operators and several examples since their usage is slightly more complex than
the previous examples.
Note that you should avoid reducing a stream of items into collections for
the sake of it. It can undermine the benefits of reactive programming
where items are processed in a beginning-to-end, one-at-a-time sequence.
You only want to aggregate the emitted items into a collection when you
need to group them logically in some way.
toList()
The toList() is probably the most often used among all the collection operators. For a
given Observable<T>, it collects incoming items into a List<T> and then pushes that
List<T> object as a single value through Single<List<T>.
Basic Operators
Chapter 3
[ 87 ]
In the following code snippet, we collect String values into a List<String>. After the
preceding Observable signals onComplete(), that list is pushed into the Observer to be
printed:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_30 {
public static void main(String[] args) {
Observable.just("Alpha", "Beta", "Gamma")
.toList()
.subscribe(s -> System.out.println("Received: " + s));
}
}
The output of the preceding code snippet is as follows:
Received: [Alpha, Beta, Gamma]
By default, toList() uses an ArrayList implementation of the List interface. You can
optionally specify an integer argument to serve as the capacityHint value that optimizes
the initialization of the ArrayList to expect roughly that number of items:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_31 {
public static void main(String[] args) {
Observable.range(1, 1000)
.toList(1000)
.subscribe(s -> System.out.println("Received: " + s));
}
}
If you want to use a different List implementation, you can provide a Callable function
as an argument to specify one. In the following code snippet, we provide a
CopyOnWriteArrayList instance to serve as a List implementation:
import io.reactivex.rxjava3.core.Observable;
import java.util.concurrent.CopyOnWriteArrayList;
public class Ch3_32 {
public static void main(String[] args) {
Observable.just("Beta", "Gamma", "Alpha")
.toList(CopyOnWriteArrayList::new)
.subscribe(s -> System.out.println("Received: " + s));
}
}
Basic Operators
Chapter 3
[ 88 ]
The result of the preceding code appears as follows:
Received: [Beta, Gamma, Alpha]
If you want to use Google Guava's immutable list, this is a little trickier since it is
immutable and uses a builder. We will show you how to do this while discussing the
collect() operator later in this section.
toSortedList()
A different flavor of toList() operator is toSortedList(). It collects the emitted values
into a List object that has the elements sorted in a natural order (based on their
Comparable implementation). Then, it pushes that List<T> object with sorted elements
into the Observer:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_33 {
public static void main(String[] args) {
Observable.just("Beta", "Gamma", "Alpha")
.toSortedList()
.subscribe(s -> System.out.println("Received: " + s));
}
}
The output of the preceding code snippet is as follows:
Received: [Alpha, Beta, Gamma]
As with the sorted() operator, you can provide a Comparator as an argument to apply a
different sorting logic. You can also specify an initial capacity for the backing ArrayList,
just like in the case of the toList() operator.
toMap() and toMultiMap()
For a given Observable<T>, the toMap() operator collects received values into Map<K,T>,
where K is the key type. The key is generated by the Function<T,K> function provided as
the argument. For example, if we want to collect strings into Map<Char, String>, where
each string is keyed off their first character, we can do it like this:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_34 {
Basic Operators
Chapter 3
[ 89 ]
public static void main(String[] args) {
Observable.just("Alpha", "Beta", "Gamma")
.toMap(s -> s.charAt(0))
.subscribe(s -> System.out.println("Received: " + s));
}
}
The output of the preceding code snippet is as follows:
Received: {A=Alpha, B=Beta, G=Gamma}
The s -> s.charAt(0) lambda argument takes each received String value and derives
the key to pair it with. In this case, we are making the first character of each String value
the key.
If we decide to yield a different value other than the received one to associate with the key,
we can provide a second lambda argument that maps each received value to a different
one. We can, for instance, map each first letter key with the length of the received String
object:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_35 {
public static void main(String[] args) {
Observable.just("Alpha", "Beta", "Gamma")
.toMap(s -> s.charAt(0), String::length)
.subscribe(s -> System.out.println("Received: " + s));
}
}
The output of the preceding code snippet is as follows:
Received: {A=5, B=4, G=5}
By default, toMap() uses the HashMap class as the Map interface implementation. You can
also provide a third argument to specify a different Map implementation. For instance, we
can provide ConcurrentHashMap instead of HashMap as the desired implementation of the
Map interface:
import io.reactivex.rxjava3.core.Observable;
import java.util.concurrent.ConcurrentHashMap;
public class Ch3_36 {
public static void main(String[] args) {
Observable.just("Alpha", "Beta", "Gamma")
.toMap(s -> s.charAt(0), String::length,
ConcurrentHashMap::new)
Basic Operators
Chapter 3
[ 90 ]
.subscribe(s -> System.out.println("Received: " + s));
}
}
Note that if there is a key that maps to multiple received values, the last value for that key
is going to replace the previous ones. For example, let's make the string length the key for
each received value. Then, Alpha is going to be replaced by Gamma:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_37 {
public static void main(String[] args) {
Observable.just("Alpha", "Beta", "Gamma")
.toMap(String::length)
.subscribe(s -> System.out.println("Received: " + s));
}
}
The output of the preceding code snippet is as follows:
Received: {4=Beta, 5=Gamma}
If you want a given key to map to multiple values, you can use toMultiMap() instead,
which maintains a list of corresponding values for each key. The items Alpha and Gamma
will then all be put in a list that is keyed off the length 5:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_38 {
public static void main(String[] args) {
Observable.just("Alpha", "Beta", "Gamma")
.toMultimap(String::length)
.subscribe(s -> System.out.println("Received: " + s));
}
}
The output of the preceding code snippet is as follows:
Received: {4=[Beta], 5=[Alpha, Gamma]}
collect()
When none of the collection operators can do what you need, you can always use the
collect() operator to specify a different type to collect items into.
Basic Operators
Chapter 3
[ 91 ]
For instance, there is no toSet() operator to collect emissions in a Set<T>, but you can
quickly use collect(Callable<U> initialValueSupplier, BiConsumer<U,T>
collector) to effectively do this.
Let's say you need to collect String values in a Set<String> implementation. To
accomplish that, you can specify the first argument—the function that produces an initial
value of the Set<String> implementation you would like to use, and the second
argument—the function that is going to collect the values (whatever you need to collect) in
that Set<String> implementation you have chosen. Here is the code that uses
HashSet<String> as the Set<String> implementation:
import io.reactivex.rxjava3.core.Observable;
import java.util.HashSet;
public class Ch3_39 {
public static void main(String[] args) {
Observable.just("Alpha", "Beta", "Gamma", "Beta")
.collect(HashSet<String>::new, HashSet::add)
.subscribe(s -> System.out.println("Received: " + s));
}
}
The output of the preceding code snippet is as follows:
Received: [Gamma, Alpha, Beta]
The collect() operator in our example now emits a single HashSet<String> object
containing all the emitted values, except the duplicates (note that Beta was emitted twice
by the source Observable); that is the nature of the HashSet class.
When you need to collect values into a mutable object and you need a new mutable object
seed each time, use collect() instead of the reduce() operator.
You can also use collect() for trickier cases that are not straightforward collection
implementations. For example, let's assume you have added Google Guava as a
dependency (https:/​/​github.​com/​google/​guava):
<dependency>
<groupId>com.google.guava</groupId>
<artifactId>guava</artifactId>
<version>28.2-jre</version>
</dependency>
Basic Operators
Chapter 3
[ 92 ]
You did it because you want to collect values in
com.google.common.collect.ImmutableList. To create an ImmutableList , you
have to call its builder() factory to yield an ImmutableList.Builder<T>. You then call
its add() method to put items in the builder, followed by a call to build(), which returns
a sealed final ImmutableList<T> that cannot be modified.
To accomplish that, you can supply an ImmutableList.Builder<T> for your first lambda
argument and then add each element through its add() method in the second argument.
This will emit ImmutableList.Builder<T> once it is fully populated, and you can
transform it using the map() operator and its build() method, which produces the
ImmutableList<T> object. Here is the code that does just that:
import com.google.common.collect.ImmutableList;
import io.reactivex.rxjava3.core.Observable;
public class Ch3_40 {
public static void main(String[] args) {
Observable.just("Alpha", "Beta", "Gamma")
.collect(ImmutableList::builder, ImmutableList.Builder::add)
.map(ImmutableList.Builder::build)
.subscribe(s -> System.out.println("Received: " + s));
}
}
The output of the preceding code snippet is as follows:
Received: [Alpha, Beta, Gamma]
Again, the collect() operator is helpful to collect emissions into any type, when the
Observable operators do not provide out-of-the-box.
Error recovery operators
Exceptions can occur almost anywhere in the chain of the Observable operators, and we
already know about the onError event that is communicated down the Observable chain
to the Observer. After that, the subscription terminates and no more emissions occur.
But sometimes, we want to intercept exceptions before they get to the Observer and
attempt some form of recovery. We can also pretend that the error never happened and
expect to continue processing the emissions.
Basic Operators
Chapter 3
[ 93 ]
However, a more productive approach to error handling would be to attempt resubscribing
or switch to an alternate source Observable. And if you find that none of the error
recovery operators meet your needs, the chances are you can compose one yourself.
For demonstration examples, let's divide 10 by each emitted integer value, where one of the
values is 0. This will result in a / by zero exception being pushed to the Observer, as we
saw in the Observable.fromCallable() section in Chapter 2, Observable and Observer (examples
Ch2_26a and Ch_26b). Here is another example:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_41 {
public static void main(String[] args) {
Observable.just(5, 2, 4, 0, 3)
.map(i -> 10 / i)
.subscribe(i -> System.out.println("RECEIVED: " + i),
e -> System.out.println("RECEIVED ERROR: " + e));
}
}
The output of the preceding code snippet is as follows:
RECEIVED: 2
RECEIVED: 5
RECEIVED: 2
RECEIVED ERROR: java.lang.ArithmeticException: / by zero
onErrorReturnItem() and onErrorReturn()
When you want to resort to a default value when an exception occurs, you can use the
onErrorReturnItem() operator. If we want to emit -1 when an exception occurs, we can
do it like this:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_42 {
public static void main(String[] args) {
Observable.just(5, 2, 4, 0, 3)
.map(i -> 10 / i)
.onErrorReturnItem(-1)
.subscribe(i -> System.out.println("RECEIVED: " + i),
e -> System.out.println("RECEIVED ERROR: " + e));
}
}
Basic Operators
Chapter 3
[ 94 ]
The output of the preceding code snippet is as follows:
RECEIVED: 2
RECEIVED: 5
RECEIVED: 2
RECEIVED: -1
You can see that the emissions stopped after the error anyway, but the error itself did not
flow down to the Observer. Instead, the value -1 was received by it as if emitted by the
source Observable.
You can also use the onErrorReturn(Function<Throwable,T> valueSupplier)
operator to dynamically produce the value using the specified function. This gives you
access to a Throwable object, which you can use while calculating the returned value as
shown in the following code snippet:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_43 {
public static void main(String[] args) {
Observable.just(5, 2, 4, 0, 3)
.map(i -> 10 / i)
.onErrorReturn(e ->
e instanceof ArithmeticException ? -1 : 0)
.subscribe(i -> System.out.println("RECEIVED: " + i),
e -> System.out.println("RECEIVED ERROR: " + e));
}
}
The location of onErrorReturn() in the chain of the operators matters. If we put it before
the map() operator in our example, the error would not be caught because it happened
downstream. To intercept the emitted error, it must originate upstream from the
onErrorReturn() operator.
Note that, again, although we handled the error, the emission was still terminated after
that. We did not get the 3 that was supposed to follow. If you want to resume emissions,
you can handle the error within the map() operator where the error occurs. You would do
this in lieu of onErrorReturn() or onErrorReturnItem():
import io.reactivex.rxjava3.core.Observable;
public class Ch3_44 {
public static void main(String[] args) {
Observable.just(5, 2, 4, 0, 3)
.map(i -> {
try {
Basic Operators
Chapter 3
[ 95 ]
return 10 / i;
} catch (ArithmeticException e) {
return -1;
}
})
.subscribe(i -> System.out.println("RECEIVED: " + i),
e -> System.out.println("RECEIVED ERROR: " + e));
}
}
The output of the preceding code snippet is as follows:
RECEIVED: 2
RECEIVED: 5
RECEIVED: 2
RECEIVED: -1
RECEIVED: 3
onErrorResumeWith()
Similar to onErrorReturn() and onErrorReturnItem(), the onErrorResumeWith()
operator (previously called onErrorResuumeNext() in RxJava 2.x) handles the exception
too. The only difference is that it accepts another Observable as a parameter to emit
potentially multiple values, not a single value, in the event of an exception.
This is somewhat contrived and likely has no business use case, but we can emit three -1
values in the event of an error:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_45 {
public static void main(String[] args) {
Observable.just(5, 2, 4, 0, 3)
.map(i -> 10 / i)
.onErrorResumeWith(Observable.just(-1).repeat(3))
.subscribe(i -> System.out.println("RECEIVED: " + i),
e -> System.out.println("RECEIVED ERROR: " + e));
}
}
Basic Operators
Chapter 3
[ 96 ]
The output of the preceding code snippet is as follows:
RECEIVED: 2
RECEIVED: 5
RECEIVED: 2
RECEIVED: -1
RECEIVED: -1
RECEIVED: -1
We can also provide Observable.empty() to quietly stop emissions in the event that
there is an error and gracefully call the onComplete() function:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_46 {
public static void main(String[] args) {
Observable.just(5, 2, 4, 0, 3)
.map(i -> 10 / i)
.onErrorResumeWith(Observable.empty())
.subscribe(i -> System.out.println("RECEIVED: " + i),
e -> System.out.println("RECEIVED ERROR: " + e));
}
}
The output of the preceding code snippet is as follows:
RECEIVED: 2
RECEIVED: 5
RECEIVED: 2
Instead of another Observable, you can provide the
Function<Throwable,Observable<T>> function to produce an Observable
dynamically from the emitted Throwable, as shown in the following code snippet:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_47 {
public static void main(String[] args) {
Observable.just(5, 2, 4, 0, 3)
.map(i -> 10 / i)
.onErrorResumeNext((Throwable e) ->
Observable.just(-1).repeat(3))
.subscribe(i -> System.out.println("RECEIVED: " + i),
e -> System.out.println("RECEIVED ERROR: " + e));
}
}
Basic Operators
Chapter 3
[ 97 ]
The output of the preceding code is as follows:
RECEIVED: 2
RECEIVED: 5
RECEIVED: 2
RECEIVED: -1
RECEIVED: -1
RECEIVED: -1
retry()
Another way to attempt recovery is to use the retry() operator, which has several
overloaded versions. It will re-subscribe to the preceding Observable and, hopefully, not
have the error again.
If you call retry() with no arguments, it will resubscribe an infinite number of times for
each error. You need to be careful with retry() without parameters as it can have chaotic
effects. Using it with our example will cause it to emit these integers infinitely and
repeatedly:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_48 {
public static void main(String[] args) {
Observable.just(5, 2, 4, 0, 3)
.map(i -> 10 / i)
.retry()
.subscribe(i -> System.out.println("RECEIVED: " + i),
e -> System.out.println("RECEIVED ERROR: " + e));
}
}
The output of the preceding code snippet is as follows:
RECEIVED: 5
RECEIVED: 2
RECEIVED: 2
RECEIVED: 5
RECEIVED: 2
RECEIVED: 2
RECEIVED: 5
RECEIVED: 2
...
Basic Operators
Chapter 3
[ 98 ]
It might be safer to specify retry() a fixed number of times before it gives up and just
emits the error to the Observer. In the following code snippet, we retry two times:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_49 {
public static void main(String[] args) {
Observable.just(5, 2, 4, 0, 3, 2, 8)
.map(i -> 10 / i)
.retry(2)
.subscribe(i -> System.out.println("RECEIVED: " + i),
e -> System.out.println("RECEIVED ERROR: " + e));
}
}
The output of the preceding code snippet is as follows:
RECEIVED: 2
RECEIVED: 5
RECEIVED: 2
RECEIVED: 2
RECEIVED: 5
RECEIVED: 2
RECEIVED: 2
RECEIVED: 5
RECEIVED: 2
RECEIVED ERROR: java.lang.ArithmeticException: / by zero
You can also provide the Predicate<Throwable> or
BiPredicate<Integer,Throwable> function to conditionally control when retry() is
attempted.
The retryUntil(BooleanSupplier stop) operator allows retries as long as the
specified BooleanSupplier function returns false.
There is also an advanced retryWhen() operator that supports advanced composition for
tasks such as delaying retries.
Basic Operators
Chapter 3
[ 99 ]
Action operators
The following are some helpful operators that can assist in debugging as well as getting
visibility into an Observable chain. These are the action or doOn operators. They do not
modify the Observable, but use it for side effects.
doOnNext() and doAfterNext()
The three operators, doOnNext(), doOnComplete(), and doOnError(), are like putting a
mini Observer right in the middle of the Observable chain.
The doOnNext() operator allows a peek at each received value before letting it flow into
the next operator. The doOnNext() operator does not affect the processing or transform the
emission in any way. We can use it just to create a side effect for each received value. For
instance, we can perform an action with each String object before it is mapped to its
length. In this case, we just print them by providing a Consumer<T> function as a lambda
expression:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_50 {
public static void main(String[] args) {
Observable.just("Alpha", "Beta", "Gamma")
.doOnNext(s -> System.out.println("Processing: " + s))
.map(String::length)
.subscribe(i -> System.out.println("Received: " + i));
}
}
The output of the preceding code snippet is as follows:
Processing: Alpha
Received: 5
Processing: Beta
Received: 4
Processing: Gamma
Received: 5
Basic Operators
Chapter 3
[ 100 ]
You can also leverage doAfterNext(), which performs the action after the item is passed
downstream rather than before. The demo code of doAfterNext() appears as follows:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_51 {
public static void main(String[] args) {
Observable.just("Alpha", "Beta", "Gamma")
.doAfterNext(s -> System.out.println("After: " + s))
.map(String::length)
.subscribe(i -> System.out.println("Received: " + i));
}
}
The output is as follows:
Received: 5
After: Alpha
Received: 4
After: Beta
Received: 5
After: Gamma
doOnComplete() and doOnError()
The onComplete() operator allows you to fire off an action when an onComplete event is
emitted at the point in the Observable chain. This can be helpful in seeing which points of
the Observable chain have completed, as shown in the following code snippet:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_52 {
public static void main(String[] args) {
Observable.just("Alpha", "Beta", "Gamma")
.doOnComplete(() ->
System.out.println("Source is done emitting!"))
.map(String::length)
.subscribe(i -> System.out.println("Received: " + i));
}
}
Basic Operators
Chapter 3
[ 101 ]
The output of the preceding code snippet is as follows:
Received: 5
Received: 4
Received: 5
Source is done emitting!
And, of course, onError() will peek at the error being emitted up the chain, and you can
perform an action with it. This can be helpful to put between operators to see which one is
to blame for an error:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_53 {
public static void main(String[] args) {
Observable.just(5, 2, 4, 0, 3, 2, 8)
.doOnError(e -> System.out.println("Source failed!"))
.map(i -> 10 / i)
.doOnError(e -> System.out.println("Division failed!"))
.subscribe(i -> System.out.println("RECEIVED: " + i),
e -> System.out.println("RECEIVED ERROR: " + e));
}
}
The output of the preceding code snippet is as follows:
RECEIVED: 2
RECEIVED: 5
RECEIVED: 2
Division failed!
RECEIVED ERROR: java.lang.ArithmeticException: / by zero
We used doOnError() in two places to see where the error first appeared. Since we did not
see Source failed! printed, but we saw Division failed!, we can deduce that the
error occurred in the map() operator.
Use these three operators together to get an insight into what your Observable operation
is doing or to quickly create side effects.
There is also a doOnTerminate() operator, which fires for an
onComplete or onError event (but before the event), and the
doAfterTerminate(), which fires for an onComplete or onError event
too, but only after the event.
Basic Operators
Chapter 3
[ 102 ]
doOnEach()
The doOnEach() operator is very similar to doOnNext(). The only difference is that in
doOnEach(), the emitted item comes wrapped inside a Notification that also contains
the type of the event. This means you can check which of the three events—onNext(),
onComplete(), or onError()—has happened and select an appropriate action.
The subscribe() method accepts these three actions as lambda arguments or an entire
Observer<T>. So, using doOnEach() is like putting subscribe() right in the middle of
your Observable chain! Here is an example:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_54 {
public static void main(String[] args) {
Observable.just("One", "Two", "Three")
.doOnEach(s -> System.out.println("doOnEach: " + s))
.subscribe(i -> System.out.println("Received: " + i));
}
}
The output is as follows:
doOnEach: OnNextNotification[One]
Received: One
doOnEach: OnNextNotification[Two]
Received: Two
doOnEach: OnNextNotification[Three]
Received: Three
doOnEach: OnCompleteNotification
As you can see, the event is wrapped inside OnNextNotification in this case. You can
check the event type, as shown in the following code:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_55 {
public static void main(String[] args) {
Observable.just("One", "Two", "Three")
.doOnEach(s -> System.out.println("doOnEach: " +
s.isOnNext() + ", " + s.isOnError() +
", " + s.isOnComplete()))
.subscribe(i -> System.out.println("Received: " + i));
}
}
Basic Operators
Chapter 3
[ 103 ]
The output looks like this:
doOnEach: true, false, false
Received: One
doOnEach: true, false, false
Received: Two
doOnEach: true, false, false
Received: Three
doOnEach: false, false, true
The error and the value (the emitted item) can be extracted from Notification in the
same way as shown in the following code snippet:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_56 {
public static void main(String[] args) {
Observable.just("One", "Two", "Three")
.doOnEach(s -> System.out.println("doOnEach: " +
s.getError() + ", " + s.getValue()))
.subscribe(i -> System.out.println("Received: " + i));
}
}
The output looks like this:
doOnEach: null, One
Received: One
doOnEach: null, Two
Received: Two
doOnEach: null, Three
Received: Three
doOnEach: null, null
doOnSubscribe() and doOnDispose()
Two other helpful action operators are doOnSubscribe() and doOnDispose().
doOnSubscribe(Consumer<Disposable> onSubscribe) executes the function
provided at the moment subscription occurs. It provides access to the Disposable object in
case you want to call dispose() in that action. The doOnDispose(Action onDispose)
operator performs the specified action when disposal is executed.
We use both operators to print when subscription and disposal occur, as shown in the
following code snippet. Then, the emitted values go through, and then disposal is finally
fired.
Basic Operators
Chapter 3
[ 104 ]
Let's now try and see how these operators are called:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_57 {
public static void main(String[] args) {
Observable.just("Alpha", "Beta", "Gamma")
.doOnSubscribe(d -> System.out.println("Subscribing!"))
.doOnDispose(() -> System.out.println("Disposing!"))
.subscribe(i -> System.out.println("RECEIVED: " + i));
}
}
The output of the preceding code snippet is as follows:
Subscribing!
RECEIVED: Alpha
RECEIVED: Beta
RECEIVED: Gamma
As you could predict, we set the subscribe event to fire off first, but doOnDispose() was
not called. That is because the dispose() method was not called. Let's do this then:
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import java.util.concurrent.TimeUnit;
public class Ch3_58 {
public static void main(String[] args) {
Disposable disp = Observable.interval(1, TimeUnit.SECONDS)
.doOnSubscribe(d -> System.out.println("Subscribing!"))
.doOnDispose(() -> System.out.println("Disposing!"))
.subscribe(i -> System.out.println("RECEIVED: " + i));
sleep(3000);
disp.dispose();
sleep(3000);
}
This time, we see that doOnDispose() was called:
Subscribing!
RECEIVED: Alpha
RECEIVED: Beta
RECEIVED: Gamma
Disposing!
Basic Operators
Chapter 3
[ 105 ]
Another option is to use the doFinally() operator, which will fire after either
onComplete() or onError() is called or disposed of by the chain. We will demonstrate
how this works shortly.
doOnSuccess()
Remember that Maybe and Single types do not have an onNext() event, but rather an
onSuccess() operator to pass a single emission. The doOnSuccess() operator usage
should effectively feel like doOnNext():
import io.reactivex.rxjava3.core.Observable;
public class Ch3_59 {
public static void main(String[] args) {
Observable.just(5, 3, 7)
.reduce((total, next) -> total + next)
.doOnSuccess(i -> System.out.println("Emitting: " + i))
.subscribe(i -> System.out.println("Received: " + i));
}
}
The output of the preceding code snippet is as follows:
Emitting: 15
Received: 15
doFinally()
The doFinally() operator is executed when onComplete(), onError(), or disposal
happens. It is executed under the same conditions as doAfterTerminate(), plus it is also
executed after the disposal. For example, look at the following code:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_60 {
public static void main(String[] args) {
Observable.just("One", "Two", "Three")
.doFinally(() -> System.out.println("doFinally!"))
.doAfterTerminate(() ->
System.out.println("doAfterTerminate!"))
.subscribe(i -> System.out.println("Received: " + i));
}
}
Basic Operators
Chapter 3
[ 106 ]
The output is as follows:
Received: One
Received: Two
Received: Three
doAfterTerminate!
doFinally!
Now, let's see how they work when dispose() is called:
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import java.util.concurrent.TimeUnit;
public class Ch3_61 {
public static void main(String[] args) {
Disposable disp = Observable.interval(1, TimeUnit.SECONDS)
.doOnSubscribe(d -> System.out.println("Subscribing!"))
.doOnDispose(() -> System.out.println("Disposing!"))
.doFinally(() -> System.out.println("doFinally!"))
.doAfterTerminate(() ->
System.out.println("doAfterTerminate!"))
.subscribe(i -> System.out.println("RECEIVED: " + i));
sleep(3000);
disp.dispose();
sleep(3000);
}
}
The output is as follows:
Subscribing!
RECEIVED: 0
RECEIVED: 1
RECEIVED: 2
Disposing!
doFinally!
The doFinally() operator guarantees that the action is executed exactly once per
subscription.
And, by the way, the location of these operators in the chain does not matter, because they
are driven by the events, not by the emitted data. For example, we can put them in the
chain in the opposite order:
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
Basic Operators
Chapter 3
[ 107 ]
import java.util.concurrent.TimeUnit;
public class Ch3_62 {
public static void main(String[] args) {
Disposable disp = Observable.interval(1, TimeUnit.SECONDS)
.doAfterTerminate(() ->
System.out.println("doAfterTerminate!"))
.doFinally(() -> System.out.println("doFinally!"))
.doOnDispose(() -> System.out.println("Disposing!"))
.doOnSubscribe(d -> System.out.println("Subscribing!"))
.subscribe(i -> System.out.println("RECEIVED: " + i));
sleep(3000);
disp.dispose();
sleep(3000);
}
}
Yet, the output remains the same:
Subscribing!
RECEIVED: 0
RECEIVED: 1
RECEIVED: 2
Disposing!
doFinally!
Utility operators
To close this chapter, we will cover some helpful operators that have diverse functionality
that cannot be captured under the specific functional title.
delay()
We can postpone emissions using the delay() operator. It will hold any received
emissions and delay each one for the specified time period. If we wanted to delay emissions
by 3 seconds, we could do it like this:
import io.reactivex.rxjava3.core.Observable;
import java.util.concurrent.TimeUnit;
public class Ch3_63 {
public static void main(String[] args) {
DateTimeFormatter f = DateTimeFormatter.ofPattern("MM:ss");
Basic Operators
Chapter 3
[ 108 ]
System.out.println(LocalDateTime.now().format(f));
Observable.just("Alpha", "Beta", "Gamma")
.delay(3, TimeUnit.SECONDS)
.subscribe(s -> System.out.println(LocalDateTime.now()
.format(f) + " Received: " + s));
sleep(5000);
}
}
The output of the preceding code snippet is as follows (the first column is the current time
of the hour in minutes and seconds):
02:26
02:29 Received: Alpha
02:29 Received: Beta
02:29 Received: Gamma
As you can see, the emission from the source Observable was delayed by 3 seconds. You
can pass an optional third boolean argument indicating whether you want to delay error
notifications as well.
Because delay() operates on a different scheduler (such as Observable.interval()),
we need to use the sleep(long ms) method to keep the application alive long enough to
see this happen (5 seconds in our case). We described the implementation of the
sleep(long ms) method in the Observable.interval() section of Chapter 2, Observable and
Observer.
For more advanced cases, you can pass another Observable as your delay() argument,
and this will delay emissions until that other Observable emits something.
Note that there is a delaySubscription() operator, which will delay
subscribing to the Observable preceding it rather than delaying each
individual emission.
repeat()
The repeat() operator will repeat subscription after onComplete() a specified number of
times. For instance, we can repeat the emissions twice for the given Observable by passing
2 as an argument for repeat(), as shown in the following code snippet:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_64 {
Basic Operators
Chapter 3
[ 109 ]
public static void main(String[] args) {
Observable.just("Alpha", "Beta", "Gamma")
.repeat(2)
.subscribe(s -> System.out.println("Received: " + s));
}
}
The output of the preceding code snippet is as follows:
Received: Alpha
Received: Beta
Received: Gamma
Received: Alpha
Received: Beta
Received: Gamma
If you do not specify a number, it will repeat infinitely, forever re-subscribing after every
onComplete(). There is also a repeatUntil() operator that accepts a BooleanSupplier
function and continues repeating until the provided function returns true.
single( )
The single() operator returns a Single that emits the item emitted by this Observable.
If the Observable emits more than one item, the single() operator throws an exception.
If the Observable emits no item, the Single, produced by the single() operator, emits
the item passed to the operator as a parameter. Here is an example:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_65 {
public static void main(String[] args) {
Observable.just("One")
.single("Four")
.subscribe(i -> System.out.println("Received: " + i));
}
}
The output is as follows:
Received: One
Basic Operators
Chapter 3
[ 110 ]
Now, let's make sure that nothing gets to the single() operator by filtering out all the
items using the following code:
import io.reactivex.rxjava3.core.Observable;
public class Ch3_66 {
public static void main(String[] args) {
Observable.just("One", "Two", "Three")
.filter(s -> s.contains("z"))
.single("Four")
.subscribe(i -> System.out.println("Received: " + i));
}
}
The output is as follows:
Received: Four
There is also a singleElement() operator that returns Maybe when the
Observable emits one item or nothing and throws an exception
otherwise. And there is a singleOrError() operator that returns
Single when the Observable emits one item only and throws an
exception otherwise.
timestamp( )
The timestamp() operator attaches a timestamp to every item emitted by an Observable,
as shown in the following code:
import io.reactivex.rxjava3.core.Observable;
import java.util.concurrent.TimeUnit;
public class Ch3_67 {
public static void main(String[] args) {
Observable.just("One", "Two", "Three")
.timestamp(TimeUnit.SECONDS)
.subscribe(i -> System.out.println("Received: " + i));
}
}
The output is as follows:
Received: Timed[time=1561694750, unit=SECONDS, value=One]
Received: Timed[time=1561694750, unit=SECONDS, value=Two]
Received: Timed[time=1561694750, unit=SECONDS, value=Three]
Basic Operators
Chapter 3
[ 111 ]
As you can see, the results are wrapped inside the object of the Timed class, which provide
accessors to the values that we can unwrap as follows:
import io.reactivex.rxjava3.core.Observable;
import java.util.concurrent.TimeUnit;
public class Ch3_68 {
public static void main(String[] args) {
Observable.just("One", "Two", "Three")
.timestamp(TimeUnit.SECONDS)
.subscribe(i -> System.out.println("Received: " +
i.time() + " " + i.unit() + " " + i.value()));
}
}
The output will then show the values in a more user-friendly format:
Received: 1561736795 SECONDS One
Received: 1561736795 SECONDS Two
Received: 1561736795 SECONDS Three
timeInterval()
The timeInterval( ) operator emits the time lapses between the consecutive emissions of
a source Observable. Here is an example:
import io.reactivex.rxjava3.core.Observable;
import java.util.concurrent.TimeUnit;
public class Ch3_69 {
public static void main(String[] args) {
Observable.interval(2, TimeUnit.SECONDS)
.doOnNext(i -> System.out.println("Emitted: " + i))
.take(3)
.timeInterval(TimeUnit.SECONDS)
.subscribe(i -> System.out.println("Received: " + i));
sleep(7000);
}
}
Basic Operators
Chapter 3
[ 112 ]
The output is as follows:
Emitted: 0
Received: Timed[time=2, unit=SECONDS, value=0]
Emitted: 1
Received: Timed[time=2, unit=SECONDS, value=1]
Emitted: 2
Received: Timed[time=2, unit=SECONDS, value=2]
And we can unwrap the values the same way we did for the timestamp() operator:
import io.reactivex.rxjava3.core.Observable;
import java.util.concurrent.TimeUnit;
public class Ch3_70 {
public static void main(String[] args) {
Observable.interval(2, TimeUnit.SECONDS)
.doOnNext(i -> System.out.println("Emitted: " + i))
.take(3)
.timeInterval(TimeUnit.SECONDS)
.subscribe(i -> System.out.println("Received: " +
i.time() + " " + i.unit() + " " + i.value()));
sleep(7000);
}
}
The output is as follows:
Emitted: 0
Received: 2 SECONDS 0
Emitted: 1
Received: 2 SECONDS 1
Emitted: 2
Received: 2 SECONDS 2
As you can see, this output essentially is the same as the previous one. The only difference
is that we have extracted the values from the object of the Timed class.
Basic Operators
Chapter 3
[ 113 ]
Summary
We covered a lot of ground in this chapter, and hopefully, by now, you are starting to see
that RxJava has a lot of practical applications. We covered various operators that suppress
and transform emissions as well as reducing them to a single emission in some form. You
learned how RxJava provides robust ways to recover from errors as well as to get visibility
into what the Observable chain is doing with action operators.
If you want to learn more about RxJava operators, there are many resources online. Marble
diagrams are a popular form of Rx documentation, visually showing how each operator
works. The rxmarbles.com (http:/​/​rxmarbles.​com) site is a popular, interactive web app
that allows you to drag marble emissions and see the affected behavior with each operator.
There is also an RxMarbles Android App (https:/​/​play.​google.​com/​store/​apps/​details?
id=​com.​moonfleet.​rxmarbles) that you can use on your Android device. Of course, you
can also see a comprehensive list of operators on the ReactiveX website (http:/​/
reactivex.​io/​documentation/​operators.​html).
Believe it or not, we have barely gotten started. This chapter only covered the basic
operators. In the coming chapters, we will cover operators that provide powerful behavior,
such as concurrency and multicasting. But before we do that, let's move on to operators that
combine observables.