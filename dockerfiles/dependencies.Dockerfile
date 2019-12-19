FROM gravitee-portal-webui/base

RUN mkdir -p /usr/build
RUN mkdir -p /usr/target
COPY ./ /usr/build
WORKDIR /usr/build
RUN rm -rf node_modules dist
RUN ./scripts/install.sh

VOLUME /usr/target
