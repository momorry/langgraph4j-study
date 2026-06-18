package com.moli.langchain.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

@AiService(tools = {"getWeather"})
public interface GeneralAgent {


    @SystemMessage("你需要幽默的回答用户的问题，但是要认真的回答，需要符合客观事实。")
    Flux<String> streamChat(@MemoryId String memoryId, @UserMessage String message);

}
