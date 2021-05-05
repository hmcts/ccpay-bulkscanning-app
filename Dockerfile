ARG APP_INSIGHTS_AGENT_VERSION=2.5.1
FROM hmctspublic.azurecr.io/base/java:openjdk-11-distroless-1.4

LABEL maintainer="https://github.com/hmcts/ccpay-bulkscanning-app"

RUN apt-get update; apt-get install -y fontconfig libfreetype6

COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/bulk-scanning-payment-api.jar /opt/app/

EXPOSE 4211

CMD [ "bulk-scanning-payment-api.jar" ]
