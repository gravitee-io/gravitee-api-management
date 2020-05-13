#-------------------------------------------------------------------------------
# Copyright (C) 2015 The Gravitee team (http://gravitee.io)
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#            http://www.apache.org/licenses/LICENSE-2.0
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
#-------------------------------------------------------------------------------
FROM graviteeio/java:8
MAINTAINER Gravitee Team <http://gravitee.io>

ARG GRAVITEEIO_VERSION=0

# Update to get support for Zip/Unzip, nc and wget
RUN apk add --update zip unzip netcat-openbsd wget

RUN wget https://dist.gravitee.io/master/dist/graviteeio-gateway-${GRAVITEEIO_VERSION}.zip --no-check-certificate -P /tmp/ \
    && unzip /tmp/graviteeio-gateway-${GRAVITEEIO_VERSION}.zip -d /opt/ \
    && rm -rf /tmp/*

ENV GRAVITEEIO_HOME /opt/graviteeio-gateway-${GRAVITEEIO_VERSION}
RUN ln -s ${GRAVITEEIO_HOME} /opt/graviteeio-gateway

RUN addgroup -g 1000 gravitee \
    && adduser -D -u 1000 -G gravitee -h ${GRAVITEEIO_HOME} gravitee \
    && chown -R gravitee:gravitee ${GRAVITEEIO_HOME}

USER 1000

WORKDIR ${GRAVITEEIO_HOME}

EXPOSE 8082
VOLUME ["/opt/graviteeio-gateway/logs"]
CMD ["./bin/gravitee"]
