package Ch2;


import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Ch2_1 {

    private static final Logger logger = LoggerFactory.getLogger(Ch2_1.class);

    public static void main(String[] args) {
        testCreate("Go", "to", "school");
        testJust("GO", "TO", "SCHOOL");
        testFromIterable(List.of("GO", "TO", "SCHOOl"));
        testCustomObserver(List.of("GO", "TO", "SCHOOl"));
    }

    static void testCreate(String... strs) {
        logger.info("test create==============>");
        Observable<String> resource = Observable.create(emitter -> {
            for (var str: strs) {
                emitter.onNext(str);
            }
            emitter.onComplete();
        });

        resource.map(String::length)
                .filter(i -> i > 5)
                .map(i -> "received: " + i)
                .forEach(System.out::println);
    }

    static void testJust(String a, String b, String c) {
        logger.info("test just==============>");
        var resource = Observable.just(a, b, c);

        resource.map(String::length)
                .filter(i -> i > 5)
                .map(i -> "received: " + i)
                .forEach(System.out::println);
    }

    static void testFromIterable(Iterable<String> iterable) {
        logger.info("test fromIterable==============>");
        var resource = Observable.fromIterable(iterable);

        resource.map(String::length)
                .filter(i -> i > 5)
                .map(i -> "received: " + i)
                .forEach(System.out::println);
    }

    static void testCustomObserver(Iterable<String> iterable) {
        var resource = Observable.fromIterable(iterable);

        var myObserver = new Observer<Integer>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                System.out.println("test custom observer==============>");
            }

            @Override
            public void onNext(@NonNull Integer integer) {
                System.out.println("received: " + integer);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onComplete() {
                System.out.println("done!");
            }
        };

        resource.map(String::length)
                .filter(i -> i > 5)
                .subscribe(myObserver);
    }
}
