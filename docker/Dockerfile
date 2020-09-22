#
# Copyright (C) 2015 The Gravitee team (http://gravitee.io)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM adoptopenjdk/openjdk11:jre-11.0.8_10-alpine
LABEL maintainer="contact@graviteesource.com"

ARG GRAVITEEIO_VERSION=0
ARG GRAVITEEIO_DOWNLOAD_URL=https://download.gravitee.io/graviteeio-apim/distributions
ENV GRAVITEEIO_HOME /opt/graviteeio-gateway

RUN apk update \
	&& apk add --update --no-cache zip unzip netcat-openbsd wget \
	&& wget ${GRAVITEEIO_DOWNLOAD_URL}/graviteeio-full-${GRAVITEEIO_VERSION}.zip --no-check-certificate -P /tmp \
    && unzip /tmp/graviteeio-full-${GRAVITEEIO_VERSION}.zip -d /tmp/ \
	&& apk del zip unzip netcat-openbsd wget \
    && mv /tmp/graviteeio-full-${GRAVITEEIO_VERSION}/graviteeio-gateway* ${GRAVITEEIO_HOME} \
    && rm -rf /tmp/* \
	&& chgrp -R 0 ${GRAVITEEIO_HOME} \
    && chmod -R g=u ${GRAVITEEIO_HOME}

WORKDIR ${GRAVITEEIO_HOME}
EXPOSE 8082
ENTRYPOINT ["./bin/gravitee"]
