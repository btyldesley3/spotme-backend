package com.spotme.domain.feedback;

import java.util.List;

public class FeedbackResult {
    private final List<String> messages;

    public FeedbackResult(List<String> messages) {
        this.messages = List.copyOf(messages);
    }

    public List<String> getMessages() {
        return messages;
    }
}
