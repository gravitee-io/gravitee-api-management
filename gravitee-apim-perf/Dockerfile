FROM node:lts-alpine3.16@sha256:458133c0915bcc120f2af5fa1b23c73a5e2315209bd0a231ad7d60d4272011a5 as builder

USER node
WORKDIR /home/node

COPY --chown=node:node package.json .
COPY --chown=node:node yarn.lock .
RUN yarn install

COPY --chown=node:node . .
RUN rm -r dist/ && yarn build


FROM grafana/k6:0.42.0@sha256:aba50200f5f832925bfc20d2fd822f522f83a6ee794233d46f8837588eba5799 as runner

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