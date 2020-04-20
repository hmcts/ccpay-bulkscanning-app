ARG APP_INSIGHTS_AGENT_VERSION=2.5.1
FROM hmctspublic.azurecr.io/base/java:openjdk-8-distroless-1.4

LABEL maintainer="https://github.com/hmcts/ccpay-bulkscanning-app"

COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/bulk-scanning-payment-api.jar /opt/app/

EXPOSE 4211

CMD [ "bulk-scanning-payment-api.jar" ]