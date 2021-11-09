package Ch2;

import io.reactivex.rxjava3.core.Observable;

public class Ch2_2 {

    public static void main(String[] args) {
        testColdObservable();
        System.out.println("=====================================");
        testConnectableObservable();
    }

    private static void testColdObservable() {
        Observable<String> source =
                Observable.just("Alpha", "Beta", "Gamma");

        source.subscribe(s -> System.out.println("Observer 1: " + s));
        source.map(String::length)
                .filter(i -> i >= 5)
                .subscribe(s -> System.out.println("Observer 2: " + s));
    }

    private static void testConnectableObservable() {
        var source = Observable.just("Alpha", "Beta", "Chalice").publish();
        source.subscribe(s -> System.out.println("observer1: " + s));
        source.map(String::length)
                .subscribe(i -> System.out.println("observer2: " + i));
        source.connect();
        source.subscribe(s -> System.out.println("observer3: " + s));
    }
}
