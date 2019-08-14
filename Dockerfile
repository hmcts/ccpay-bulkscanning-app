ARG APP_INSIGHTS_AGENT_VERSION=2.3.1
FROM hmctspublic.azurecr.io/base/java:openjdk-8-distroless-1.0

LABEL maintainer="https://github.com/hmcts/ccpay-bulkscanning-app"

COPY build/libs/bulk-scanning-payment-api.jar /opt/app/
COPY lib/applicationinsights-agent-2.3.1.jar lib/AI-Agent.xml /opt/app/

EXPOSE 4211
CMD [ "bulk-scanning-payment-api.jar" ]
