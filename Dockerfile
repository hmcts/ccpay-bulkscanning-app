ARG APP_INSIGHTS_AGENT_VERSION=2.5.1
FROM debian:10 AS builder
RUN apt update
RUN apt install --yes libharfbuzz-dev

FROM hmctspublic.azurecr.io/base/java:openjdk-11-distroless-1.4
COPY --from=builder /usr/lib/x86_64-linux-gnu/libharfbuzz.so.0 /usr/lib/x86_64-linux-gnu/libharfbuzz.so.0
COPY --from=builder /usr/lib/x86_64-linux-gnu/libglib-2.0.so.0 /usr/lib/x86_64-linux-gnu/libglib-2.0.so.0
COPY --from=builder /usr/lib/x86_64-linux-gnu/libgraphite2.so.3 /usr/lib/x86_64-linux-gnu/libgraphite2.so.3

LABEL maintainer="https://github.com/hmcts/ccpay-bulkscanning-app"


COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/bulk-scanning-payment-api.jar /opt/app/

EXPOSE 4211

CMD [ "bulk-scanning-payment-api.jar" ]
