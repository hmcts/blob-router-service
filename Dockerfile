ARG APP_INSIGHTS_AGENT_VERSION=3.4.8

# Application image

FROM hmctspublic.azurecr.io/base/java:17-distroless

COPY build/libs/blob-router-service.jar /opt/app/
COPY lib/applicationinsights.json /opt/app/

EXPOSE 8584
CMD [ "blob-router-service.jar" ]
