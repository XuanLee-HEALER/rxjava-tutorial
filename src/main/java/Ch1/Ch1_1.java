package Ch1;

import io.reactivex.rxjava3.core.Observable;

import java.util.concurrent.TimeUnit;

public class Ch1_1 {

    public static void main(String[] args) {
        // push
        Observable<String> myStrings = Observable.just("Test1", "Test2", "Test3");
        // lambda is Observer
        myStrings.map(String::length)
                .subscribe(System.out::println);

        Observable<Long> secondInterval = Observable.interval(1, TimeUnit.SECONDS);
        secondInterval.subscribe(System.out::println);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
