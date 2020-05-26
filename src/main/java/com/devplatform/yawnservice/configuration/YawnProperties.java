package com.devplatform.yawnservice.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "yawn")
public class YawnProperties {

    private String routingkeyPrefix;

    public String getRoutingkeyPrefix() {
        return routingkeyPrefix;
    }

    public void setRoutingkeyPrefix(String routingKeyPrefix) {
        this.routingkeyPrefix = routingKeyPrefix;
    }
}