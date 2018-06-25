package de.dfki.nlp.config;

import de.dfki.nlp.domain.ParsedInputText;
import de.dfki.nlp.domain.PredictionResult;
import de.dfki.nlp.domain.rest.ServerRequest;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Transformers;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.Header;

import java.util.Set;

@Configuration
@IntegrationComponentScan
public class MessagingConfig {

    public final static String queueName = "input";
    public final static String queueNameDnorm = "dnorm";
    public final static String queueOutput = "results";

    @Bean
    Queue inputQueue() {
        return new Queue(queueName, true);
    }

    @Bean
    Queue outputQueue() {
        return new Queue(queueOutput, true);
    }

    @Bean
    public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    @ServiceActivator(inputChannel = "requestChannel")
    public AmqpOutboundEndpoint amqpOutbound(AmqpTemplate amqpTemplate) {
        AmqpOutboundEndpoint outbound = new AmqpOutboundEndpoint(amqpTemplate);
        outbound.setRoutingKey(queueName); // default exchange - route to queue 'queuename'
        outbound.setExpectReply(false);
        outbound.setHeaderMapper(DefaultAmqpHeaderMapper.outboundMapper());
        // handle the priority
        return outbound;
    }

    @Bean
    @ServiceActivator(inputChannel = "dnormChannel")
    public AmqpOutboundEndpoint dnormOutbound(AmqpTemplate amqpTemplate) {
        AmqpOutboundEndpoint outbound = new AmqpOutboundEndpoint(amqpTemplate);
        outbound.setRoutingKey(queueNameDnorm); // default exchange - route to queue 'queuename'
        outbound.setExpectReply(true);
        outbound.setHeaderMapper(DefaultAmqpHeaderMapper.outboundMapper());
        return outbound;
    }


    @Bean
    public MessageChannel amqpOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel responseChannel() {
        return new DirectChannel();
    }

    @MessagingGateway(
            defaultRequestChannel = "requestChannel"
    )
    public interface ProcessingGateway {

        String HEADER_REQUEST_TIME = "requestTime";

        @Gateway(replyTimeout = Long.MAX_VALUE, requestTimeout = Long.MAX_VALUE)
        void sendForProcessing(ServerRequest data,
                               @Header(AmqpHeaders.EXPIRATION) String ttlInMs,
                               @Header(HEADER_REQUEST_TIME) long now);
    }

    @MessagingGateway(
            defaultRequestChannel = "dnormChannel"
    )
    public interface DNormGateway {

        @Gateway(replyTimeout = Long.MAX_VALUE, requestTimeout = Long.MAX_VALUE)
        String sendForProcessing(ParsedInputText data);
    }

    @Bean
    public MessageChannel seth() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mirner() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel diseases() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel banner() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel linnaeus() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel parsed() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel dnorm() {
        return new DirectChannel();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter jackson2Converter) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2Converter);
        return rabbitTemplate;
    }

    @Bean
    public MappingJackson2MessageConverter consumerJackson2MessageConverter() {
        return new MappingJackson2MessageConverter();
    }


}
