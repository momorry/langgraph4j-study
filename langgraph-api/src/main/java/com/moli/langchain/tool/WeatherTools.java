package com.moli.langchain.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class WeatherTools {
    @Tool("Returns the weather forecast for a given city")
    public String getWeather(@P("The city for which the weather forecast should be returned") String city) {
        return city+"25℃";
    }

}
