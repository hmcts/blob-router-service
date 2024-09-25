ARG APP_INSIGHTS_AGENT_VERSION=3.5.2

# Application image

FROM hmctspublic.azurecr.io/base/java:17-distroless-debug

COPY build/libs/blob-router-service.jar /opt/app/
COPY lib/applicationinsights.json /opt/app/

EXPOSE 8584
CMD [ "blob-router-service.jar" ]
