package com.textify.me.service;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PubSubPublisherService {

    private final ObjectMapper objectMapper; // For JSON serialization/deserialization

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${spring.cloud.gcp.pubsub.topic-id}")
    private String topicId;
	 
	private Publisher publisher; // The actual Pub/Sub client publisher

    public PubSubPublisherService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
	// Initialize the Publisher client after properties are injected
    @PostConstruct
    public void initializePublisher() {
        try {
            TopicName topicName = TopicName.of(projectId, topicId);
            this.publisher = Publisher.newBuilder(topicName).build();
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize Pub/Sub Publisher", e);
        }
    }

    public void publishProcessingRequest(String jobId, String originalGcsPath) {
        try {
            // Create a payload object
            ProcessingRequestPayload payload = new ProcessingRequestPayload(jobId, originalGcsPath);
            // Convert the payload object to a JSON string
            String jsonPayload = objectMapper.writeValueAsString(payload);

            // Build the PubsubMessage
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8(jsonPayload))
                // You can also add attributes (key-value pairs) if needed, e.g., for routing or filtering
                // .putAttributes("fileType", "image")
                .build();

            // Publish the message asynchronously
            publisher.publish(pubsubMessage);
                    } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create Pub/Sub message payload", e);
        } catch (Exception e) { // Catch broader exceptions from publisher.publish()
            throw new RuntimeException("Failed to publish to Pub/Sub", e);
        }
    }

    // Ensure the publisher client is properly shut down when the application closes
    @PreDestroy
    public void shutdownPublisher() {
        if (publisher != null) {
            try {
                publisher.shutdown();
                publisher.awaitTermination(1, java.util.concurrent.TimeUnit.MINUTES);
            } catch (Exception e) {
                
            }
        }
    }

    private static class ProcessingRequestPayload {
        public String jobId;
        public String originalGcsPath;

        public ProcessingRequestPayload(String jobId, String originalGcsPath) {
            this.jobId = jobId;
            this.originalGcsPath = originalGcsPath;
        }
    }
}

