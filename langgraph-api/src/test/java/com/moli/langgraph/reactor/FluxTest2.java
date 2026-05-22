package com.moli.langgraph.reactor;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author likethewind
 * @since 2026/5/20 14:29
 *
 */
public class FluxTest2 {

    public static void main(String[] args) {

        Flux.just("Hello", "World").subscribe(System.out::println);
        Flux.fromArray(new Integer[]{1, 2, 3}).subscribe(System.out::println);
        Flux.empty().subscribe(System.out::println);
        Flux.range(1, 10).subscribe(System.out::println);

        Flux.range(1, 1000).take(10).subscribe(System.out::println);
        System.out.println();
        Flux.range(1, 1000).takeLast(10).subscribe(System.out::println);
        System.out.println();
        Flux.range(1, 1000).takeWhile(i -> i < 10).subscribe(System.out::println);
        System.out.println();
        Flux.range(1, 1000).takeUntil(i -> i == 10).subscribe(System.out::println);
        System.out.println();

        Flux.range(1, 100).reduce((x, y) -> x + y).subscribe(System.out::println);
        System.out.println();
        Flux.range(1, 100).reduceWith(() -> 100, (x,y)->(x+y)).subscribe(System.out::println);


//        Flux.range(1, 10).subscribe(System.out::println);

        Flux.interval(Duration.of(50, ChronoUnit.MILLIS))
                .take(10)
                .map(i -> i + 1)
                .subscribe(System.out::println);
        System.out.println("---");
        Flux.range(1, 10)
                .delayElements(Duration.of(50, ChronoUnit.MILLIS))
                .subscribe(System.out::println);

        System.out.println("==========");
        final Random random = new Random();
        Flux.generate(ArrayList::new, (list, sink) -> {
            int value = random.nextInt(100);
            list.add(value);
            sink.next(value);
            if( list.size() ==10 )
                sink.complete();
            return list;
        }).subscribe(System.out::println);

        System.out.println("+++++++");
        Flux.create(sink -> {
            for(int i = 0; i < 10; i ++)
                sink.next(i);
            sink.complete();
        }).subscribe(x-> System.out.println("create:" + x));

        System.out.println("--sech");
        Flux.create(sink -> {
                    for(int i = 0; i < 10; i ++)
                        sink.next(i+"::"+Thread.currentThread().getName());
                    sink.complete();
                }).publishOn(Schedulers.single())
                .map(x ->  String.format("[%s] %s", Thread.currentThread().getName(), x))
                .publishOn(Schedulers.boundedElastic())
                .map(x -> String.format("[%s] %s", Thread.currentThread().getName(), x))
                .subscribeOn(Schedulers.parallel())
                .toStream()
                .forEach(x-> System.out.println("Schedulers:" + x));

        while (true);
    }
}
