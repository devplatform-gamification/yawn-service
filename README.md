# dev-platform
Set of services to use in a developers platform. E.g. webhook-listener, translator, event-logger, scores, gamification-server, bots

## yawn-service
This service is a client to webhooks which are integrated with dev-platform, actually supports:
- jira -- issue created + issue updated
- gitlab -- merge request, new branchs, changes in a merge request
- slack -- comments

It receives all those webhooks and sends to the rabbitMQ with a specific service routingkey.

## Docker image

`$ echo [pass] | sudo docker login docker.pkg.github.com -u [user] --password-stdin`

`$ sudo docker pull docker.pkg.github.com/devplatform-gamification/yawn-service/yawn-service:latest`

`$ sudo docker run docker.pkg.github.com/devplatform-gamification/yawn-service/yawn-service:latest -p 8020:8020`

## Environment variables

- RABBITMQ_HOST 	[rabbit-host] # default: localhost
- RABBIT_VHOST 	[rabbit-vhost] # default: /
- RABBIT_USERNAME 	[rabbit-user] # default: guest
- RABBIT_PASSWORD 	[rabbit-pass] # default: guest
- RABBIT_EXCHANGE 	[rabbit-exchange] # default: dev-platform.exchange
- APP_PORT [APP-PORT] # default: 8020
- RABBIT_JIRA_ROUTINGKEY_PREFIX [jira-routingkey-prefix] # default: dev-platform.jira
- RABBIT_GITLAB_ROUTINGKEY_PREFIX [gitlab-routingkey-prefix] # default: dev-platform.gitlab
- RABBIT_SLACK_ROUTINGKEY_PREFIX [slack-routingkey-prefix] # default: dev-platform.slack
