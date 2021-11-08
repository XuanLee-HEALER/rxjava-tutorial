package Ch2;


import io.reactivex.rxjava3.core.Observable;

public class Ch2_1 {

    public static void main(String[] args) {
        Observable<String> source = Observable.create(emitter -> {
            emitter.onNext("Go");
            emitter.onNext("to");
            emitter.onNext("school");
            emitter.onComplete();
        });

        source.subscribe(System.out::println, Throwable::printStackTrace);
        var nSource = source.map(s -> s.length());
        var nnSource = nSource.filter(s -> s > 5);
        nnSource.subscribe(System.out::println);
    }
}
