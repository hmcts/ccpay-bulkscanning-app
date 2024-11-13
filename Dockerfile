ARG APP_INSIGHTS_AGENT_VERSION=3.6.2

FROM hmctspublic.azurecr.io/base/java:21-distroless

COPY lib/applicationinsights.json /opt/app/

LABEL maintainer="https://github.com/hmcts/ccpay-bulkscanning-app"

COPY build/libs/bulk-scanning-payment-api.jar /opt/app/

EXPOSE 4211

CMD [ \
    "--add-opens", "java.base/java.lang=ALL-UNNAMED", \
    "bulk-scanning-payment-api.jar" \
    ]
