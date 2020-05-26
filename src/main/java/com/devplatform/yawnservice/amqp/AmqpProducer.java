package com.devplatform.yawnservice.amqp;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.devplatform.yawnservice.configuration.YawnProperties;

@Component
public class AmqpProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    YawnProperties yawnProperties;
    
	@Value("${spring.application.name}")
	private String appName;
	@Value("${spring.application.version}")
	private String appVersion;
    
    public void sendMessage(Notification msg){
        System.out.println("Send msg = " + msg.toString());
        rabbitTemplate.convertAndSend(yawnProperties.getRoutingkeyPrefix(), msg);
    }

    public void sendMessageGeneric(Object msg, String routingkeyPrefix, String eventType){
    	String routingKey = routingkeyPrefix
    							.concat(".")
    							.concat(eventType);
        System.out.println("Send Generic msg with routingkey: ["+ routingKey + "] = " + msg.toString());
        rabbitTemplate.convertAndSend(routingKey ,msg);
    }
}