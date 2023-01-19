ARG APP_INSIGHTS_AGENT_VERSION=2.5.1

# Application image

FROM hmctspublic.azurecr.io/base/java:17-distroless

COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/blob-router-service.jar /opt/app/

EXPOSE 8584
CMD [ "blob-router-service.jar" ]
