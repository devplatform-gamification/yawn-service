package com.devplatform.yawnservice.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.devplatform.model.ModelApiResponse;
import com.devplatform.model.jira.JiraIssue;
import com.devplatform.model.jira.event.JiraEvent;
import com.devplatform.model.jira.event.JiraEventComment;
import com.devplatform.model.jira.event.JiraEventIssue;
import com.devplatform.model.jira.event.JiraEventLink;
import com.devplatform.model.jira.event.JiraEventVersion;
import com.devplatform.yawnservice.amqp.AmqpProducer;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.ApiParam;

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2020-03-18T19:01:01.992Z[GMT]")
@Controller
public class JiraApiController implements JiraApi {

	private static final Logger log = LoggerFactory.getLogger(JiraApiController.class);

	private final ObjectMapper objectMapper;

	private final HttpServletRequest request;

	@Value("${spring.rabbitmq.template.custom.jira.routing-key-prefix}")
	private String routingKeyPrefix;

	@Autowired
	private AmqpProducer amqpProducer;

	@org.springframework.beans.factory.annotation.Autowired
	public JiraApiController(ObjectMapper objectMapper, HttpServletRequest request) {
		this.objectMapper = objectMapper;
		this.request = request;
	}

	@Override
	public ResponseEntity<ModelApiResponse> events(
			@RequestParam(name = "user_id") String userId, @RequestParam(name = "user_key") String userKey,
			@ApiParam(value = "", required = true) @RequestBody byte[] bodyBytes) {
		
		log.info(new String(bodyBytes));
		JiraEvent bodyGeneric;
		try {
			bodyGeneric = objectMapper.readValue(bodyBytes, JiraEvent.class);
			if (bodyGeneric.getWebhookEvent() != null) {
				switch (bodyGeneric.getWebhookEvent()) {
				case ISSUE_CREATED:
				case ISSUE_UPDATED:
				case ISSUE_DELETED:
					JiraEventIssue issueEvent = objectMapper.readValue(bodyBytes, JiraEventIssue.class);
					issueEvent.setUserKey(userKey);
					issueEvent.setUserName(userId);
					return addUpdateIssue(issueEvent);
				case ISSUE_LINK_CREATED:
				case ISSUE_LINK_DELETED:
					JiraEventLink issueLinkEvent = objectMapper.readValue(bodyBytes, JiraEventLink.class);
					issueLinkEvent.setUserKey(userKey);
					issueLinkEvent.setUserName(userId);
					return addDeleteIssueLink(issueLinkEvent);
				case COMMENT_CREATED:
				case COMMENT_UPDATED:
				case COMMENT_DELETED:
					JiraEventComment commentEvent = objectMapper.readValue(bodyBytes, JiraEventComment.class);
					commentEvent.setUserKey(userKey);
					commentEvent.setUserName(userId);
					return addUpdateIssueComment(commentEvent);
				case VERSION_CREATED:
				case VERSION_UPDATED:
				case VERSION_MOVED:
				case VERSION_RELEASED:
				case VERSION_UNRELEASED:
				case VERSION_DELETED:
					JiraEventVersion versionEvent = objectMapper.readValue(bodyBytes, JiraEventVersion.class);
					versionEvent.setUserKey(userKey);
					versionEvent.setUserName(userId);
					return projectVersion(versionEvent);
				default:
					log.error("Não foi possível identificar o tipo de evento recebido: " + bodyGeneric.getWebhookEvent().name());
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			log.error("Couldn't desserialize request", e);
			return new ResponseEntity<ModelApiResponse>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		log.error("[JIRA] The answer will be not implemented (501)");
		return new ResponseEntity<ModelApiResponse>(HttpStatus.NOT_IMPLEMENTED);
	}
	
	public ResponseEntity<ModelApiResponse> addUpdateIssue(
			@ApiParam(value = "", required = true) @Valid @RequestBody JiraEventIssue body) {
		String eventType = "";
		if(body.getWebhookEvent() != null && StringUtils.isNotBlank(body.getWebhookEvent().name())) {
			eventType = body.getWebhookEvent().name();
		}else {
			log.error("Couldn't get the eventy type for message: " + body.toString());
		}
		if(body.getIssueEventTypeName() != null) {
			eventType = eventType.concat(".").concat(body.getIssueEventTypeName().name());
		}
		String issueRountingKeySuffix = getIssueRoutingKeySuffix(body.getIssue());
		if(StringUtils.isNotBlank(issueRountingKeySuffix)) {
			eventType = eventType.concat(".").concat(issueRountingKeySuffix);
		}

		try {
			amqpProducer.sendMessageGeneric(body, routingKeyPrefix, eventType);
			return new ResponseEntity<ModelApiResponse>(objectMapper.readValue(
					"{\n  \"code\" : 0,\n  \"type\" : \"type\",\n  \"message\" : \"message\"\n}",
					ModelApiResponse.class), HttpStatus.OK);
		} catch (IOException e) {
			log.error("Couldn't serialize response for content type application/json", e);
			return new ResponseEntity<ModelApiResponse>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	/**
	 * Get issue identification: ISSUETYPEID.STATUSID.PROJECTKEY.ISSUEKEY
	 * @param issue
	 * @return
	 */
	private String getIssueRoutingKeySuffix(JiraIssue issue) {
		String routingKeySuffix = null;
		List<String> issueIdentification = new ArrayList<>();
		if(issue != null) {
			if(issue.getFields() != null) {
				if(issue.getFields().getIssuetype() != null ) {
					issueIdentification.add(issue.getFields().getIssuetype().getId().toString());
				}
				if(issue.getFields().getStatus() != null ) {
					issueIdentification.add(issue.getFields().getStatus().getId().toString());
				}
				if(issue.getFields().getProject() != null ) {
					issueIdentification.add(issue.getFields().getProject().getKey());
				}
			}
			if(issue.getKey() != null ) {
				issueIdentification.add(issue.getKey());
			}
		}
		if(issueIdentification != null && !issueIdentification.isEmpty()) {
			routingKeySuffix = String.join(".", issueIdentification);
		}
		
		return routingKeySuffix;
	}

	@Override
	public ResponseEntity<ModelApiResponse> addDeleteIssueLink(
			@ApiParam(value = "", required = true) @Valid @RequestBody JiraEventLink body) {
		try {
			amqpProducer.sendMessageGeneric(body, routingKeyPrefix, body.getWebhookEvent().name());
			return new ResponseEntity<ModelApiResponse>(objectMapper.readValue(
					"{\n  \"code\" : 0,\n  \"type\" : \"type\",\n  \"message\" : \"message\"\n}",
					ModelApiResponse.class), HttpStatus.OK);
		} catch (IOException e) {
			log.error("Couldn't serialize response for content type application/json", e);
			return new ResponseEntity<ModelApiResponse>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ResponseEntity<ModelApiResponse> addUpdateIssueComment( 
			@ApiParam(value = "", required = true) @Valid @RequestBody JiraEventComment body) {

		try {
			amqpProducer.sendMessageGeneric(body, routingKeyPrefix, body.getWebhookEvent().name());
			return new ResponseEntity<ModelApiResponse>(objectMapper.readValue(
					"{\n  \"code\" : 0,\n  \"type\" : \"type\",\n  \"message\" : \"message\"\n}",
					ModelApiResponse.class), HttpStatus.OK);
		} catch (IOException e) {
			log.error("Couldn't serialize response for content type application/json", e);
			return new ResponseEntity<ModelApiResponse>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ResponseEntity<ModelApiResponse> projectVersion(
			@ApiParam(value = "", required = true) @Valid @RequestBody JiraEventVersion body) {

		try {
			amqpProducer.sendMessageGeneric(body, routingKeyPrefix, body.getWebhookEvent().name());
			return new ResponseEntity<ModelApiResponse>(objectMapper.readValue(
					"{\n  \"code\" : 0,\n  \"type\" : \"type\",\n  \"message\" : \"message\"\n}",
					ModelApiResponse.class), HttpStatus.OK);
		} catch (IOException e) {
			log.error("Couldn't serialize response for content type application/json", e);
			return new ResponseEntity<ModelApiResponse>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}	
}
