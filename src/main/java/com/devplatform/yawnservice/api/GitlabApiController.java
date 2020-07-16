package com.devplatform.yawnservice.api;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.devplatform.model.ModelApiResponse;
import com.devplatform.model.gitlab.event.GitlabEvent;
import com.devplatform.model.gitlab.event.GitlabEventMergeRequest;
import com.devplatform.model.gitlab.event.GitlabEventNote;
import com.devplatform.model.gitlab.event.GitlabEventPush;
import com.devplatform.model.gitlab.event.GitlabEventPushTag;
import com.devplatform.yawnservice.amqp.AmqpProducer;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.ApiParam;

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2020-03-18T19:01:01.992Z[GMT]")
@Controller
public class GitlabApiController implements GitlabApi {

	private static final Logger log = LoggerFactory.getLogger(GitlabApiController.class);

	private final ObjectMapper objectMapper;

	private final HttpServletRequest request;

	@Value("${spring.rabbitmq.template.custom.gitlab.routing-key-prefix}")
	private String routingKeyPrefix;
	
	@Value("${yawn.gitlab.authentication-header-name}")
	private String REQUEST_HEADER_NAME;

	@Value("${yawn.gitlab.authentication-header-value}")
	private String REQUEST_HEADER_VALUE;

	@Autowired
	private AmqpProducer amqpProducer;

	@org.springframework.beans.factory.annotation.Autowired
	public GitlabApiController(ObjectMapper objectMapper, HttpServletRequest request) {
		this.objectMapper = objectMapper;
		this.request = request;
	}

	@Override
	public ResponseEntity<ModelApiResponse> events(@ApiParam(value = "", required = true) @RequestBody byte[] bodyBytes,
			@RequestHeader HttpHeaders headers) {

		if(checkGitlabEventsHeader(headers)) {
			log.info(new String(bodyBytes));
			GitlabEvent bodyGeneric;
			try {
				bodyGeneric = objectMapper.readValue(bodyBytes, GitlabEvent.class);
				if (bodyGeneric.getObjectKind() != null) {
					switch (bodyGeneric.getObjectKind()) {
					case MERGE_REQUEST:
						GitlabEventMergeRequest mergeRequestEvent = objectMapper.readValue(bodyBytes,
								GitlabEventMergeRequest.class);
						return mergeRequest(mergeRequestEvent);
					case PUSH:
						GitlabEventPush pushEvent = objectMapper.readValue(bodyBytes, GitlabEventPush.class);
						return push(pushEvent);
					case TAG_PUSH:
						GitlabEventPushTag tagEvent = objectMapper.readValue(bodyBytes, GitlabEventPushTag.class);
						return tag(tagEvent);
					case COMMENT:
						GitlabEventNote noteEvent = objectMapper.readValue(bodyBytes, GitlabEventNote.class);
						return comment(noteEvent);
					default:
						log.error("Não foi possível identificar o tipo de evento recebido: "
								+ bodyGeneric.getObjectKind().name());
						break;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				log.error("Couldn't desserialize request", e);
				return new ResponseEntity<ModelApiResponse>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			log.error("[GITLAB] The answer will be not implemented (501)");
			return new ResponseEntity<ModelApiResponse>(HttpStatus.NOT_IMPLEMENTED);
		}
		return new ResponseEntity<ModelApiResponse>(HttpStatus.UNAUTHORIZED);
	}

	@Override
	public ResponseEntity<ModelApiResponse> mergeRequest(
			@ApiParam(value = "", required = true) @Valid @RequestBody GitlabEventMergeRequest body) {

		try {
			amqpProducer.sendMessageGeneric(body, routingKeyPrefix, body.getEventType().name());
			return new ResponseEntity<ModelApiResponse>(objectMapper.readValue(
					"{\n  \"code\" : 0,\n  \"type\" : \"type\",\n  \"message\" : \"message\"\n}",
					ModelApiResponse.class), HttpStatus.OK);
		} catch (IOException e) {
			log.error("Couldn't serialize response for content type application/json", e);
			return new ResponseEntity<ModelApiResponse>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ResponseEntity<ModelApiResponse> push(
			@ApiParam(value = "", required = true) @Valid @RequestBody GitlabEventPush body) {

		try {
			amqpProducer.sendMessageGeneric(body, routingKeyPrefix, body.getEventName().name());
			return new ResponseEntity<ModelApiResponse>(objectMapper.readValue(
					"{\n  \"code\" : 0,\n  \"type\" : \"type\",\n  \"message\" : \"message\"\n}",
					ModelApiResponse.class), HttpStatus.OK);
		} catch (IOException e) {
			log.error("Couldn't serialize response for content type application/json", e);
			return new ResponseEntity<ModelApiResponse>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@Override
	public ResponseEntity<ModelApiResponse> tag(
			@ApiParam(value = "", required = true) @Valid GitlabEventPushTag body) {
		try {
			amqpProducer.sendMessageGeneric(body, routingKeyPrefix, body.getEventName().name());
			return new ResponseEntity<ModelApiResponse>(objectMapper.readValue(
					"{\n  \"code\" : 0,\n  \"type\" : \"type\",\n  \"message\" : \"message\"\n}",
					ModelApiResponse.class), HttpStatus.OK);
		} catch (IOException e) {
			log.error("Couldn't serialize response for content type application/json", e);
			return new ResponseEntity<ModelApiResponse>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ResponseEntity<ModelApiResponse> comment(
			@ApiParam(value = "", required = true) @Valid @RequestBody GitlabEventNote body) {

		try {
			amqpProducer.sendMessageGeneric(body, routingKeyPrefix, body.getEventType().name());
			return new ResponseEntity<ModelApiResponse>(objectMapper.readValue(
					"{\n  \"code\" : 0,\n  \"type\" : \"type\",\n  \"message\" : \"message\"\n}",
					ModelApiResponse.class), HttpStatus.OK);
		} catch (IOException e) {
			log.error("Couldn't serialize response for content type application/json", e);
			return new ResponseEntity<ModelApiResponse>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	private boolean checkGitlabEventsHeader(HttpHeaders headers) {
		boolean autorizado = false;
		List<String> tokenList = headers.get(REQUEST_HEADER_NAME);
		if(tokenList != null && tokenList.size() > 0) {
			if(tokenList.get(0) != null) {
				autorizado = tokenList.get(0).equals(REQUEST_HEADER_VALUE);
			}
		}
		return autorizado;
	}
}