package com.synapse.learning.srs.application.port.out;

public interface KafkaDlqPort {

    void publish(String originalTopic, String partitionKey, String payload);
}
