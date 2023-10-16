FROM node:lts-alpine3.16@sha256:f47850733d8522489f57bfe86d790b1ee167a4b863d83d37572fb28cf10ec5e8 as builder

USER node
WORKDIR /home/node

COPY --chown=node:node package.json .
COPY --chown=node:node yarn.lock .
RUN yarn install

COPY --chown=node:node . .
RUN rm -r dist/ && yarn build

FROM mostafamoradian/xk6-kafka:0.17.0 as runner

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