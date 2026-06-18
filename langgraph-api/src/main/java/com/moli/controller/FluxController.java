package com.moli.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.TimeUnit;

@RestController
public class FluxController {


    @GetMapping(value = "/flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Object> flux() {
//        return Flux.just(1, 2, 3).map(x -> "" + x).delayElements(Duration.ofSeconds(2));
        return Flux.create(sink->{
            for (int i = 0; i < 10; i++) {
                sink.next(i+"");
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            sink.complete();
        }).subscribeOn(Schedulers.boundedElastic()).log();
    }

}
