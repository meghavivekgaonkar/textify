package com.textify.worker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;

@Service
public class PubSubMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(PubSubMessageListener.class);
    private final WorkerProcessingService workerProcessingService;
    private final ObjectMapper objectMapper;

    public PubSubMessageListener(WorkerProcessingService workerProcessingService, ObjectMapper objectMapper) {
        this.workerProcessingService = workerProcessingService;
        this.objectMapper = objectMapper;
    }

    // Listen to messages from the configured Pub/Sub subscription
    @ServiceActivator(inputChannel = "pubsubInputChannel")
    public void receiveMessage(String payload, // The message data as a String (JSON payload)
            @Header(GcpPubSubHeaders.ORIGINAL_MESSAGE) BasicAcknowledgeablePubsubMessage message) {
        logger.info("Received message: {}", payload);
        try {

            // Deserialize the JSON payload into our processing request DTO
            ProcessingRequestPayload request = objectMapper.readValue(payload, ProcessingRequestPayload.class);

            // Delegate the actual heavy lifting to the WorkerProcessingService
            workerProcessingService.processJobWithRetry(request.jobId);

            // Acknowledge the message if processing was successful
            message.ack();
            logger.info("Message for job ID {} acknowledged successfully.", request.jobId);

        } catch (Exception e) {
            logger.error("Error processing Pub/Sub message for payload '{}': {}", payload, e.getMessage(), e);
            // Negative acknowledge the message if an error occurred.
            // This tells Pub/Sub to redeliver the message later (with backoff).
            message.nack();
            logger.warn("Message for job ID {} negatively acknowledged.", getJobIdFromPayload(payload));
        }
    }

    // Helper to extract jobId from payload string for logging on nack
    private String getJobIdFromPayload(String payload) {
        try {
            return objectMapper.readTree(payload).get("jobId").asText();
        } catch (JsonProcessingException e) {
            return "unknown";
        }
    }

    // This DTO defines the structure of the message received from Pub/Sub
    // Must match the payload structure from PubSubPublisherService in api-service
    private static class ProcessingRequestPayload {
        public String jobId;
        public String originalGcsPath;

        // Required by Jackson for deserialization
        public ProcessingRequestPayload() {
        }

        public ProcessingRequestPayload(String jobId, String originalGcsPath) {
            this.jobId = jobId;
            this.originalGcsPath = originalGcsPath;
        }
    }
}