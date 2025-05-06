FROM node:lts-alpine3.16@sha256:1eabdb1aa3f6e68558c08501572731db560d10c2194a67d3417103b0a9868693 as builder

USER node
WORKDIR /home/node

COPY --chown=node:node package.json .
COPY --chown=node:node yarn.lock .
RUN yarn install

COPY --chown=node:node . .
RUN rm -r dist/ && yarn build

FROM mostafamoradian/xk6-kafka:0.31.5 as runner

USER root
RUN apk add --update --no-cache jq util-linux bash
USER k6

RUN mkdir -p /tmp/perf_report/k6 \
    && chown -R 12345:12345 /tmp/perf_report/k6

COPY --chown=12345:12345 scripts scripts
COPY --chown=12345:12345 scripts/entrypoint.sh /usr/bin/entrypoint.sh
RUN chmod +x /usr/bin/entrypoint.sh

COPY --from=builder --chown=12345:12345 /home/node/dist/ dist

ENTRYPOINT ["/usr/bin/entrypoint.sh"]
