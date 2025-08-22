package blog.eric231.examples.multiconnector.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for Multi-Connector Pipeline Example
 * 
 * This configuration class sets up the necessary beans and infrastructure
 * for the multi-connector pipeline example, including RabbitMQ queues,
 * message converters, and thread pool configuration.
 */
@Configuration
@EnableAsync
public class MultiConnectorConfig {

    /**
     * Primary ObjectMapper bean for JSON processing
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return mapper;
    }

    /**
     * RabbitMQ Message Converter using Jackson
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * Configure RabbitTemplate with JSON message converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, 
                                       Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    /**
     * RabbitAdmin for queue management
     */
    @Bean
    @ConditionalOnProperty(name = "spring.rabbitmq.host")
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    /**
     * Exchange for pipeline notifications
     */
    @Bean
    @ConditionalOnProperty(name = "spring.rabbitmq.host")
    public DirectExchange pipelineNotificationsExchange() {
        return ExchangeBuilder.directExchange("pipeline.notifications.exchange")
                .durable(true)
                .build();
    }

    /**
     * Queue for pipeline notifications
     */
    @Bean
    @ConditionalOnProperty(name = "spring.rabbitmq.host")
    public Queue pipelineNotificationsQueue() {
        return QueueBuilder.durable("pipeline.notifications")
                .withArgument("x-message-ttl", 86400000) // 24 hours TTL
                .build();
    }

    /**
     * Binding for pipeline notifications
     */
    @Bean
    @ConditionalOnProperty(name = "spring.rabbitmq.host")
    public Binding pipelineNotificationsBinding(Queue pipelineNotificationsQueue,
                                               DirectExchange pipelineNotificationsExchange) {
        return BindingBuilder.bind(pipelineNotificationsQueue)
                .to(pipelineNotificationsExchange)
                .with("pipeline.notifications");
    }

    /**
     * Exchange for file events
     */
    @Bean
    @ConditionalOnProperty(name = "spring.rabbitmq.host")
    public TopicExchange fileEventsExchange() {
        return ExchangeBuilder.topicExchange("file.events.exchange")
                .durable(true)
                .build();
    }

    /**
     * Queue for file events
     */
    @Bean
    @ConditionalOnProperty(name = "spring.rabbitmq.host")
    public Queue fileEventsQueue() {
        return QueueBuilder.durable("file.events")
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    /**
     * Binding for file events
     */
    @Bean
    @ConditionalOnProperty(name = "spring.rabbitmq.host")
    public Binding fileEventsBinding(Queue fileEventsQueue, TopicExchange fileEventsExchange) {
        return BindingBuilder.bind(fileEventsQueue)
                .to(fileEventsExchange)
                .with("file.#");
    }

    /**
     * Dead Letter Exchange for failed messages
     */
    @Bean
    @ConditionalOnProperty(name = "spring.rabbitmq.host")
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange("pipeline.dlx")
                .durable(true)
                .build();
    }

    /**
     * Dead Letter Queue
     */
    @Bean
    @ConditionalOnProperty(name = "spring.rabbitmq.host")
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("pipeline.failed")
                .build();
    }

    /**
     * Dead Letter Binding
     */
    @Bean
    @ConditionalOnProperty(name = "spring.rabbitmq.host")
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with("failed");
    }

    /**
     * Exchange for IBM MQ integration
     */
    @Bean
    @ConditionalOnProperty(name = "spring.rabbitmq.host")
    public DirectExchange ibmMQIntegrationExchange() {
        return ExchangeBuilder.directExchange("ibmmq.integration.exchange")
                .durable(true)
                .build();
    }

    /**
     * Queue for IBM MQ notifications
     */
    @Bean
    @ConditionalOnProperty(name = "spring.rabbitmq.host")
    public Queue ibmMQNotificationsQueue() {
        return QueueBuilder.durable("ibmmq.notifications")
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    /**
     * Binding for IBM MQ notifications
     */
    @Bean
    @ConditionalOnProperty(name = "spring.rabbitmq.host")
    public Binding ibmMQNotificationsBinding(Queue ibmMQNotificationsQueue,
                                            DirectExchange ibmMQIntegrationExchange) {
        return BindingBuilder.bind(ibmMQNotificationsQueue)
                .to(ibmMQIntegrationExchange)
                .with("ibmmq.notifications");
    }

    /**
     * Queue for IBM MQ triggers
     */
    @Bean
    @ConditionalOnProperty(name = "spring.rabbitmq.host")
    public Queue ibmMQTriggersQueue() {
        return QueueBuilder.durable("ibmmq.triggers")
                .withArgument("x-message-ttl", 1800000) // 30 minutes TTL
                .build();
    }

    /**
     * Binding for IBM MQ triggers
     */
    @Bean
    @ConditionalOnProperty(name = "spring.rabbitmq.host")
    public Binding ibmMQTriggersBinding(Queue ibmMQTriggersQueue,
                                       DirectExchange ibmMQIntegrationExchange) {
        return BindingBuilder.bind(ibmMQTriggersQueue)
                .to(ibmMQIntegrationExchange)
                .with("ibmmq.triggers");
    }

    /**
     * Thread Pool for async pipeline operations
     */
    @Bean(name = "pipelineExecutor")
    public Executor pipelineExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("Pipeline-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Thread Pool for file event processing
     */
    @Bean(name = "fileEventExecutor")
    public Executor fileEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("FileEvent-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
