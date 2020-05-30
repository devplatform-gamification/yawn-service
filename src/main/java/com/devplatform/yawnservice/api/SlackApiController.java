package com.devplatform.yawnservice.api;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;

import com.devplatform.model.ModelApiResponse;
import com.devplatform.model.event.slack.SlackChannelMessage;
import com.devplatform.yawnservice.amqp.AmqpProducer;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.ApiParam;
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2020-03-18T19:01:01.992Z[GMT]")
@Controller
public class SlackApiController implements SlackApi {

    private static final Logger log = LoggerFactory.getLogger(SlackApiController.class);

    private final ObjectMapper objectMapper;

    private final HttpServletRequest request;

	@Value("${spring.rabbitmq.template.custom.slack.routing-key-prefix}")
    private String routingKeyPrefix;
    
    @Autowired
    private AmqpProducer amqpProducer;

    @org.springframework.beans.factory.annotation.Autowired
    public SlackApiController(ObjectMapper objectMapper, HttpServletRequest request) {
        this.objectMapper = objectMapper;
        this.request = request;
    }

    public ResponseEntity<ModelApiResponse> addMessage(@ApiParam(value = "" ,required=true )  @Valid @RequestBody SlackChannelMessage body
) {
    	amqpProducer.sendMessageGeneric(body, routingKeyPrefix, body.getType().name());
    	
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<ModelApiResponse>(objectMapper.readValue("{\n  \"code\" : 0,\n  \"type\" : \"type\",\n  \"message\" : \"message\"\n}", ModelApiResponse.class), HttpStatus.ACCEPTED);
            } catch (IOException e) {
                log.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<ModelApiResponse>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<ModelApiResponse>(HttpStatus.NOT_IMPLEMENTED);
    }

}
