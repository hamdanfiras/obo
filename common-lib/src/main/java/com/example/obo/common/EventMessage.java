package com.example.obo.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EventMessage {
    private final String eventType;
    private final String oboToken;
    private final Object payload;

    @JsonCreator
    public EventMessage(
            @JsonProperty("eventType") String eventType,
            @JsonProperty("oboToken") String oboToken,
            @JsonProperty("payload") Object payload) {
        this.eventType = eventType;
        this.oboToken = oboToken;
        this.payload = payload;
    }

    public String getEventType() {
        return eventType;
    }

    public String getOboToken() {
        return oboToken;
    }

    public Object getPayload() {
        return payload;
    }
}

