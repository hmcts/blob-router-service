ARG APP_INSIGHTS_AGENT_VERSION=2.5.0

# Build image

FROM busybox as downloader

RUN wget -P /tmp https://github.com/microsoft/ApplicationInsights-Java/releases/download/${APP_INSIGHTS_AGENT_VERSION}/applicationinsights-agent-${APP_INSIGHTS_AGENT_VERSION}.jar

# Application image

FROM hmctspublic.azurecr.io/base/java:openjdk-11-distroless-1.4

COPY --from=downloader /tmp/applicationinsights-agent-${APP_INSIGHTS_AGENT_VERSION}.jar /opt/app/

COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/blob-router-service.jar /opt/app/

EXPOSE 8584
CMD [ "blob-router-service.jar" ]
