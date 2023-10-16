ARG APP_INSIGHTS_AGENT_VERSION=3.4.14
FROM debian:10 AS builder
RUN apt update
RUN apt install --yes libharfbuzz-dev=2.3.1

FROM hmctspublic.azurecr.io/base/java:17-distroless

COPY lib/applicationinsights.json /opt/app/
COPY --from=builder /usr/lib/x86_64-linux-gnu/libharfbuzz.so.0 /usr/lib/x86_64-linux-gnu/libharfbuzz.so.0
COPY --from=builder /usr/lib/x86_64-linux-gnu/libglib-2.0.so.0 /usr/lib/x86_64-linux-gnu/libglib-2.0.so.0
COPY --from=builder /usr/lib/x86_64-linux-gnu/libgraphite2.so.3 /usr/lib/x86_64-linux-gnu/libgraphite2.so.3
COPY --from=builder /lib/x86_64-linux-gnu/libpcre.so.3 /lib/x86_64-linux-gnu/libpcre.so.3

LABEL maintainer="https://github.com/hmcts/ccpay-bulkscanning-app"

COPY build/libs/bulk-scanning-payment-api.jar /opt/app/

EXPOSE 4211

CMD [ \
    "--add-opens", "java.base/java.lang=ALL-UNNAMED", \
    "bulk-scanning-payment-api.jar" \
    ]
