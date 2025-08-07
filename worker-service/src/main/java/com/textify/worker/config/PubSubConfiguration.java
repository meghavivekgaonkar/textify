package com.textify.worker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;

@Configuration
public class PubSubConfiguration {

    // This bean creates the channel your ServiceActivator is listening to
    @Bean
    public MessageChannel pubsubInputChannel() {
        return new DirectChannel();
    }

    // This bean is a Spring Integration adapter that connects your pubsubInputChannel
    // to the specified Google Cloud Pub/Sub subscription.
    @Bean
    public PubSubInboundChannelAdapter messageChannelAdapter(PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(pubSubTemplate, "textify-processing-subscription");
        adapter.setOutputChannel(pubsubInputChannel());
        return adapter;
    }
}