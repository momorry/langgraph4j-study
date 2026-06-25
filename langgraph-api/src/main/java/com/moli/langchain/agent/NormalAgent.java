package com.moli.langchain.agent;

import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

public interface NormalAgent {


    TokenStream execute(@UserMessage String input);
}
