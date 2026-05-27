package com.moli.langgraph.graph.state;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.Map;

/**
 * 教师作文批改工作流状态
 */
public class EssayReviewState extends AgentState {

    public static final String STUDENT_NAME_KEY = "studentName";
    public static final String ESSAY_TOPIC_KEY = "essayTopic";
    public static final String ESSAY_CONTENT_KEY = "essayContent";
    public static final String TEACHER_COMMENT_KEY = "teacherComment";
    public static final String ESSAY_GRADE_KEY = "essayGrade";

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            STUDENT_NAME_KEY, Channels.base(() -> ""),
            ESSAY_TOPIC_KEY, Channels.base(() -> ""),
            ESSAY_CONTENT_KEY, Channels.base(() -> ""),
            TEACHER_COMMENT_KEY, Channels.base(() -> ""),
            ESSAY_GRADE_KEY, Channels.base(() -> "")
    );

    public EssayReviewState(Map<String, Object> initData) {
        super(initData);
    }

    public String studentName() {
        return value(STUDENT_NAME_KEY).map(Object::toString).orElse("");
    }

    public String essayTopic() {
        return value(ESSAY_TOPIC_KEY).map(Object::toString).orElse("");
    }

    public String essayContent() {
        return value(ESSAY_CONTENT_KEY).map(Object::toString).orElse("");
    }

    public String teacherComment() {
        return value(TEACHER_COMMENT_KEY).map(Object::toString).orElse("");
    }

    public String essayGrade() {
        return value(ESSAY_GRADE_KEY).map(Object::toString).orElse("");
    }
}
