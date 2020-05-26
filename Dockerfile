FROM alpine:3.10

# instaling openjdk8 and bash in this image
RUN apk add --update \
    openjdk8-jre \
    bash \
  && rm -rf /var/cache/apk/*

LABEL maintainer="com.devplatform"

VOLUME /tmp

EXPOSE 8020

ENV JAVA_OPTS=""

ARG JAR_FILE=target/yawn-service*.jar
ADD ${JAR_FILE} yawn.jar

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/yawn.jar", "--logging.file=/tmp/yawn.log"]