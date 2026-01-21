package com.policia.batch.config;

import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.JMSException;

@Configuration
public class JmsConfiguration {

    @Value("${mq.host}")
    private String mqHost;

    @Value("${mq.port}")
    private int mqPort;

    @Value("${mq.channel}")
    private String channel;

    @Value("${mq.queueManager}")
    private String queueManager;

    @Value("${mq.queue.rta}")
    private String queueRta;

    @Value("${mq.queue.mas}")
    private String queueMas;

    @Bean
    public MQQueueConnectionFactory mqQueueConnectionFactory() throws JMSException {
        MQQueueConnectionFactory factory = new MQQueueConnectionFactory();
        factory.setHostName(mqHost);
        factory.setPort(mqPort);
        factory.setChannel(channel);
        factory.setQueueManager(queueManager);
        factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        
        return factory;
    }

    @Bean("jmsTemplateRta")
    public JmsTemplate jmsTemplateRta() throws JMSException {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(mqQueueConnectionFactory());
        jmsTemplate.setDefaultDestinationName(queueRta);
        jmsTemplate.setReceiveTimeout(5000);
        return jmsTemplate;
    }

    @Bean("jmsTemplateMas")  
    public JmsTemplate jmsTemplateMas() throws JMSException {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(mqQueueConnectionFactory());
        jmsTemplate.setDefaultDestinationName(queueMas);
        jmsTemplate.setReceiveTimeout(5000);
        return jmsTemplate;
    }
}
