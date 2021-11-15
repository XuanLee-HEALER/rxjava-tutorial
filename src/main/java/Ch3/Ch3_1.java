package Ch3;

import io.reactivex.rxjava3.core.Observable;

public class Ch3_1 {

    public static void main(String[] args) {
//        testTakeWhileAndSkipWhile();
        testDefaultIfEmpty();
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

    private static void printInfo(Object o) {
        System.out.println("Print => " + o);
    }
}
