package Ch3;

import io.reactivex.rxjava3.core.Observable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class Ch3_1 {

    public static void main(String[] args) {
//        testTakeWhileAndSkipWhile();
//        testDefaultIfEmpty();
//        testSwitchIfEmpty();
//        testFilter();
//        testTakeAndTakeLast();
//        testSkipAndSkipLast();
        testDistinct();
    }

    private static void testTakeWhileAndSkipWhile() {
        var source = Observable.range(1, 100);

        source.takeWhile(i -> i <= 5)
                .subscribe(Ch3_1::printInfo)
                .dispose();

        source.skipWhile(i -> i <= 95)
                .subscribe(Ch3_1::printInfo)
                .dispose();
    }

    private static void testDefaultIfEmpty() {
        var source = Observable.just("Alpha", "Bob");

        source.filter(s -> s.startsWith("C"))
                .defaultIfEmpty("None")
                .subscribe(Ch3_1::printInfo)
                .dispose();
    }

    private static void testSwitchIfEmpty() {
        Observable.just("Atm", "Atom", "At")
                .filter(s -> s.startsWith("Z"))
                .switchIfEmpty(Observable.just("Bob", "Beat"))
                .subscribe(i -> System.out.println("Received: " + i),
                        Throwable::printStackTrace)
                .dispose();
    }

    private static void testFilter() {
        Observable.just("Abed", "Puppey", "Maybe")
                .filter(s -> s.length() > 5)
                .subscribe(s -> System.out.println("Received: " + s), Throwable::printStackTrace)
                .dispose();
    }

    private static void testTakeAndTakeLast() {

//        Observable.just("A", "B", "C")
//                .take(2)
//                .subscribe(s -> System.out.println("Received: " + s))
//                .dispose();

        System.out.println("==========================");
        var formatter = DateTimeFormatter.ofPattern("ss:SSS");
        Observable.interval(300, TimeUnit.MILLISECONDS)
                .take(2, TimeUnit.SECONDS)
                .map(i -> String.format("%s Received: %d", LocalDateTime.now().format(formatter), i))
                .subscribe(System.out::println, Throwable::printStackTrace);

//        System.out.println("==========================");
//        Observable.just("A", "B", "C")
//                .takeLast(2)
//                .subscribe(s -> System.out.println("Received: " + s))
//                .dispose();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void testSkipAndSkipLast() {
        var source = Observable.range(1, 100);
        source.skip(90)
                .subscribe(Ch3_1::printInfo)
                .dispose();
        System.out.println("==========================");
        source.skipLast(10)
                .subscribe(Ch3_1::printInfo)
                .dispose();
    }

    private static void testDistinct() {
        var source = Observable.just("Adam", "Bobs", "Chalice");
        source.distinct()
                .subscribe(Ch3_1::printInfo)
                .dispose();
        System.out.println("====================");
        source.distinct(String::length)
                .subscribe(Ch3_1::printInfo)
                .dispose();
    }

    private static void printInfo(Object o) {
        System.out.println("Print => " + o);
    }
}
