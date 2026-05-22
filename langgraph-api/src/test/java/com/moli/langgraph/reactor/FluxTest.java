package com.moli.langgraph.reactor;

import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author likethewind
 * @since 2026/5/20 13:20
 *
 */
public class FluxTest {

    public static void main(String[] args) {
        create();
        convAndFilter();
        mergeAndCombain();
        handleError();
        program();
    }

    public static void create() {
        // 1. 创建静态数据流
// 创建一个包含多个元素的Flux
        Flux<String> fluxFromJust = Flux.just("Apple", "Banana", "Cherry");
        fluxFromJust.subscribe(System.out::println);

// 从集合创建Flux
        List<String> list = Arrays.asList("A", "B", "C");
        Flux<String> fluxFromIterable = Flux.fromIterable(list);
        fluxFromIterable.subscribe(System.out::println);

// 创建一个包含1到10的整数流
        Flux<Integer> rangeFlux = Flux.range(1, 10);
        rangeFlux.subscribe(System.out::println);

        System.out.println("#####create");
    }

    public static void convAndFilter() {
        // 2. 使用map和filter处理数据
        Flux<Integer> numberFlux = Flux.range(1, 10);
        numberFlux
                .map(n -> n * 2)   // 将每个元素乘以2
                .filter(n -> n > 5) // 过滤出大于5的元素
                .subscribe(System.out::println); // 输出: 6, 8, 10, ..., 20

// 3. 使用flatMap进行一对多转换
        Flux<String> names = Flux.just("Alice", "Bob");
        names.flatMap(name -> Flux.just(name + "1", name + "2"))
                .subscribe(System.out::println); // 输出: Alice1, Alice2, Bob1, Bob2

        System.out.println("#####convAndFilter");
    }

    public static void mergeAndCombain() {
        // 4. 合并多个Flux
        Flux<String> flux1 = Flux.just("A", "B");
        Flux<String> flux2 = Flux.just("C", "D");
        Flux<String> mergedFlux = Flux.merge(flux1, flux2);
        mergedFlux.subscribe(System.out::println); // 输出可能: A, C, B, D (交错)

// 5. 压缩两个Flux
        Flux<Integer> ids = Flux.just(1, 2);
        Flux<String> names = Flux.just("Alice", "Bob");
        Flux<String> zipped = Flux.zip(ids, names, (id, name) -> id + ":" + name);
        zipped.subscribe(System.out::println); // 输出: 1:Alice, 2:Bob
        System.out.println("#####mergeAndCombain");
    }

    public static void handleError() {
        // 6. 处理错误
        Flux<Integer> errorFlux = Flux.just(1, 2, 3)
                .map(i -> {
                    if (i == 2) throw new RuntimeException("Error!");
                    return i;
                })
                .onErrorReturn(-1); // 发生错误时返回默认值-1
        errorFlux.subscribe(System.out::println); // 输出: 1, -1
        System.out.println("#####handleError");
    }

    public static void program() {
        // 7. 使用Flux.create动态生成元素
        Flux<String> dynamicFlux = Flux.create(sink -> {
            // 发射数据
            sink.next("Hello");
            sink.next("World");
            // 完成数据流
            sink.complete();
            // 清理回调
            sink.onDispose(() -> System.out.println("Cleaned up!"));
        });

        dynamicFlux.subscribe(System.out::println); // 输出: Hello, World
        System.out.println("#####program");
    }
}
