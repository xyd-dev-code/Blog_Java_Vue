package com.blog.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class EmailSubscriptionSerializationTest {

    @Test
    void tokenIsNeverSerializedToFrontend() throws Exception {
        EmailSubscription subscription = new EmailSubscription();
        subscription.setEmail("user@example.com");
        subscription.setToken("private-confirmation-token");

        String json = new ObjectMapper().writeValueAsString(subscription);

        assertFalse(json.contains("token"));
        assertFalse(json.contains("private-confirmation-token"));
    }
}
