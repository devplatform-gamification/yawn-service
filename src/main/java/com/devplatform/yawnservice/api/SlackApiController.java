package com.devplatform.yawnservice.api;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;

import com.devplatform.model.ModelApiResponse;
import com.devplatform.model.slack.event.SlackEventCallback;
import com.devplatform.model.slack.event.SlackEventChallenge;
import com.devplatform.model.slack.event.SlackEventGeneric;
import com.devplatform.model.slack.event.SlackEventTypeEnum;
import com.devplatform.model.slack.response.SlackChallengeResponse;
import com.devplatform.yawnservice.amqp.AmqpProducer;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
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

	public ResponseEntity<?> events(@ApiParam(value = "", required = true) @RequestBody byte[] bodyBytes)
			throws JsonParseException, JsonMappingException, IOException {

		log.info(new String(bodyBytes));
		SlackEventGeneric bodyGeneric = objectMapper.readValue(bodyBytes, SlackEventGeneric.class);
		if (bodyGeneric.getType() != null) {
			if (bodyGeneric.getType() == SlackEventTypeEnum.URL_VERIFICATION) {
				return this.replyToChallenge(bodyBytes);
			}
			try {
				SlackEventCallback bodyCallback = objectMapper.readValue(bodyBytes, SlackEventCallback.class);
				amqpProducer.sendMessageGeneric(bodyCallback, routingKeyPrefix, bodyCallback.getType().name());
				return new ResponseEntity<ModelApiResponse>(objectMapper.readValue(
						"{\n  \"code\" : 0,\n  \"type\" : \"type\",\n  \"message\" : \"message\"\n}",
						ModelApiResponse.class), HttpStatus.OK);
			} catch (IOException e) {
				log.error("Couldn't serialize response for content type application/json", e);
				return new ResponseEntity<ModelApiResponse>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		log.error("[SLACK] The answer will be not implemented (501)");
		return new ResponseEntity<ModelApiResponse>(HttpStatus.NOT_IMPLEMENTED);
	}

	private ResponseEntity<SlackChallengeResponse> replyToChallenge(byte[] body)
			throws JsonParseException, JsonMappingException, IOException {
		SlackEventChallenge eventChallenge = objectMapper.readValue(body, SlackEventChallenge.class);
		return new ResponseEntity<SlackChallengeResponse>(new SlackChallengeResponse(eventChallenge.getChallenge()),
				HttpStatus.OK);
	}
}
