package Ch2;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Ch2_3 {

    private static List<String> list = List.of("A", "B", "C");
    private static Iterator<String> iterator = list.iterator();

    public static void main(String[] args) {
//        testRange();
//        testInterval();
//        testDefer();
        testMaybe();
    }

    private static void testRange() {
        var source = Observable.range(1, 3);
        source.subscribe(i -> System.out.println("received: " + i));

        var source1 = Observable.rangeLong(1l, 100l);
        source1.subscribe(i -> System.out.println("long received: " + i));
    }

    private static void testInterval() {
        var source = Observable.interval(1, TimeUnit.SECONDS);
        source.subscribe(l -> {
            System.out.printf("%d %d --->\n", LocalDateTime.now().getSecond(), l);
        });
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        source.subscribe(l -> {
            System.out.printf("%d %d ===>\n", LocalDateTime.now().getSecond(), l);
        });
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void testDefer() {
        var source = Observable.just(list.iterator());
        source.subscribe(itor -> {
            while (itor.hasNext()) {
                System.out.println(itor.next());
            }
            System.out.println("=======================");
        });

        // 没有消费到
        source.subscribe(itor -> {
            while (itor.hasNext()) {
                System.out.println(itor.next());
            }
        });
        System.out.println("##########################");

        iterator = list.iterator();
        var deferSource = Observable.defer(() -> Observable.just(iterator));
        deferSource.subscribe(i -> {
            while (i.hasNext()) {
                System.out.println(i.next());
            }
            System.out.println("=======================");
        });
        iterator = list.iterator();
        deferSource.subscribe(i -> {
            while (i.hasNext()) {
                System.out.println(i.next());
            }
        });
    }

    public static void testMaybe() {
        var maybe1 = Maybe.just("Go");
        var maybe2 = Maybe.empty();

        maybe1.subscribe(s -> {
            System.out.println("maybe1: " + s);
        }, e -> {
            System.err.println(e.getMessage());
        }, () -> {
            System.out.println("maybe1 done!");
        });

        maybe2.subscribe(s -> {
            System.out.println("maybe2: " + s);
        }, e -> {
            System.err.println(e.getMessage());
        }, () -> {
            System.out.println("maybe2 done!");
        });
    }
}
