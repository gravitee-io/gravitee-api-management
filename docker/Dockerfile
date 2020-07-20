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

FROM nginx:1.18-alpine
LABEL maintainer="contact@graviteesource.com"

ARG GRAVITEEIO_VERSION=0
ARG GRAVITEEIO_DOWNLOAD_URL=https://download.gravitee.io/graviteeio-apim/distributions

ENV CONFD_VERSION="0.16.0"
ENV CONFD_URL="https://github.com/kelseyhightower/confd/releases/download"

ENV WWW_TARGET /usr/share/nginx/html

RUN apk update \
  && apk add --update --no-cache zip unzip netcat-openbsd wget \
  && wget ${GRAVITEEIO_DOWNLOAD_URL}/graviteeio-full-${GRAVITEEIO_VERSION}.zip -P /tmp \
  && unzip /tmp/graviteeio-full-${GRAVITEEIO_VERSION}.zip -d /tmp/ \
  && apk del zip unzip netcat-openbsd wget \
  && cp -fr /tmp/graviteeio-full-${GRAVITEEIO_VERSION}/graviteeio-management-ui-${GRAVITEEIO_VERSION}/* ${WWW_TARGET} \
  && rm -rf /tmp/* \
  && wget -T 5 ${CONFD_URL}/v${CONFD_VERSION}/confd-${CONFD_VERSION}-linux-amd64 -O /bin/confd \
  && chmod +x /bin/confd

# Support running as arbitrary user which belongs to the root group
RUN chown -R 1001:0 /usr/share/nginx/ /var/log/nginx /var/cache/nginx/ /etc/nginx/ /var/run/

USER 1001

ADD config /etc/confd

COPY run.sh /run.sh

CMD ["sh", "/run.sh"]
