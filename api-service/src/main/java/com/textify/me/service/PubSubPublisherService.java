package com.textify.me.service;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture; // Import ApiFuture
import com.google.api.core.ApiFutureCallback; // Import ApiFutureCallback
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;


@Service
public class PubSubPublisherService {
    
    private static final Logger logger = LoggerFactory.getLogger(PubSubPublisherService.class);
    private final ObjectMapper objectMapper;
    private final Executor pubSubCallbackExecutor; // Injected Executor for callbacks

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${spring.cloud.gcp.pubsub.topic-id}")
    private String topicId;

    private Publisher publisher; // The actual Pub/Sub client publisher instance

    // Constructor to inject ObjectMapper and the custom Executor bean
    public PubSubPublisherService(ObjectMapper objectMapper,
                                  @Qualifier("pubSubCallbackExecutor") Executor pubSubCallbackExecutor) {
        this.objectMapper = objectMapper;
        this.pubSubCallbackExecutor = pubSubCallbackExecutor;
    }

    /**
     * Initializes the Pub/Sub Publisher client after Spring has injected all @Value fields.
     * This method is called automatically by Spring after bean construction and property injection.
     */
    @PostConstruct
    public void initializePublisher() {
        TopicName topicName = TopicName.of(projectId, topicId);
        try {
            // Build the Publisher instance. This is a long-lived resource.
            this.publisher = Publisher.newBuilder(topicName).build();
            logger.info("Pub/Sub Publisher initialized for topic: {}", topicName.toString());
        } catch (IOException e) {
            logger.error("Failed to initialize Pub/Sub Publisher for topic {}: {}", topicName.toString(), e.getMessage(), e);
            // This is a critical error. Throw an exception to prevent the application from starting
            // with a non-functional publisher.
            throw new IllegalStateException("Could not initialize Pub/Sub Publisher, application cannot proceed.", e);
        }
    }

    // Inner DTO for Pub/Sub payload - should match worker-service's ProcessingRequestPayload
    private static class PublishingPayload {
        public String jobId;
        public String originalGcsPath;

        public PublishingPayload(String jobId, String originalGcsPath) {
            this.jobId = jobId;
            this.originalGcsPath = originalGcsPath;
        }
    }

    /**
     * Publishes a message to Google Cloud Pub/Sub asynchronously.
     * The result (success/failure) is handled by an asynchronous callback on a dedicated executor.
     *
     * @param jobId The ID of the job to process.
     * @param originalGcsPath The GCS path of the original file.
     */
    public void publishProcessingRequest(String jobId, String originalGcsPath) {
        // Defensive check: ensure the publisher was initialized successfully
        if (publisher == null) {
            logger.error("Pub/Sub Publisher is not initialized. Cannot publish message for job ID: {}. Application startup likely failed.", jobId);
            throw new IllegalStateException("Pub/Sub Publisher is not initialized. Check application startup logs for errors.");
        }

        try {
            // Create the payload object and serialize it to JSON
            PublishingPayload payload = new PublishingPayload(jobId, originalGcsPath);
            String jsonPayload = objectMapper.writeValueAsString(payload);

            // Build the PubsubMessage with the JSON payload
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8(jsonPayload))
                .build();

            logger.info("Attempting to publish message for job ID {} to topic {}. Payload: {}",
                    jobId, topicId, jsonPayload);

            // Publish the message asynchronously. ApiFuture represents the result of the async operation.
            ApiFuture<String> future = publisher.publish(pubsubMessage);

            // Attach an asynchronous callback to handle the result (success or failure)
            // The callback will be executed on the 'pubSubCallbackExecutor' thread pool.
             ApiFutures.addCallback(future, new ApiFutureCallback<String>() {
                @Override
                public void onSuccess(String messageId) {
                    logger.info("Successfully published message for job ID {} with message ID {}.", jobId, messageId);
                }

                @Override
                public void onFailure(Throwable t) {
                    logger.error("Failed to publish message for job ID {}. Error: {}", jobId, t.getMessage(), t);
                    // You might want to log the full stack trace for more details in a real app:
                    // logger.error("Failed to publish message for job ID {}.", jobId, t);
                }
            }, pubSubCallbackExecutor); // IMPORTANT: Use the injected Executor here!

        } catch (JsonProcessingException e) {
            logger.error("Error serializing Pub/Sub message payload for job ID {}: {}", jobId, e.getMessage(), e);
            throw new RuntimeException("Failed to create Pub/Sub message payload due to JSON processing error", e);
        } catch (Exception e) {
            // Catch any other unexpected exceptions that might occur during the *initiation* of the publish operation
            logger.error("An unexpected error occurred while attempting to initiate publish for job ID {}: {}", jobId, e.getMessage(), e);
            throw new RuntimeException("Failed to initiate publish operation to Pub/Sub", e);
        }
    }

    /**
     * Shuts down the Pub/Sub Publisher client gracefully when the application context closes.
     * This method is called automatically by Spring before bean destruction.
     */
    @PreDestroy
    public void shutdownPublisher() {
        if (publisher != null) {
            try {
                logger.info("Shutting down Pub/Sub publisher for topic: {}", topicId);
                publisher.shutdown(); // Initiate graceful shutdown
                // Wait for any pending messages to be published and for the publisher to terminate
                publisher.awaitTermination(1, TimeUnit.MINUTES);
                logger.info("Pub/Sub publisher shut down successfully.");
            } catch (InterruptedException e) {
                logger.error("Interrupted while shutting down Pub/Sub publisher for topic {}: {}", topicId, e.getMessage());
                Thread.currentThread().interrupt(); // Restore the interrupted status
            } catch (Exception e) {
                logger.error("Error during Pub/Sub publisher shutdown for topic {}: {}", topicId, e.getMessage(), e);
            }
        }
    }
}