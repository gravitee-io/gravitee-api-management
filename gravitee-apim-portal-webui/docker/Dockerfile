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

FROM nginx:1.22.0-alpine
LABEL maintainer="contact@graviteesource.com"

ARG GRAVITEEIO_VERSION=0
ARG GRAVITEEIO_DOWNLOAD_URL=https://download.gravitee.io/graviteeio-apim/distributions
ARG GRAVITEEIO_PACKAGE_NAME=graviteeio-full

ENV CONFD_VERSION="0.16.0"
ENV CONFD_URL="https://github.com/kelseyhightower/confd/releases/download"

ENV WWW_TARGET /usr/share/nginx/html

RUN apk update \
  && apk add --update --no-cache zip unzip netcat-openbsd wget \
  && apk add --upgrade --no-cache libgcrypt \
  && wget ${GRAVITEEIO_DOWNLOAD_URL}/${GRAVITEEIO_PACKAGE_NAME}-${GRAVITEEIO_VERSION}.zip -P /tmp \
  && unzip /tmp/${GRAVITEEIO_PACKAGE_NAME}-${GRAVITEEIO_VERSION}.zip -d /tmp/ \
  && apk del zip unzip netcat-openbsd wget \
  && cp -fr /tmp/graviteeio-full-${GRAVITEEIO_VERSION}/graviteeio-apim-portal-ui-${GRAVITEEIO_VERSION}/* ${WWW_TARGET} \
  && rm -rf /tmp/* \
  && wget -T 5 ${CONFD_URL}/v${CONFD_VERSION}/confd-${CONFD_VERSION}-linux-amd64 -O /bin/confd \
  && chmod +x /bin/confd \
  && ln -sf /dev/stdout /var/log/nginx/access.log \
  && ln -sf /dev/stderr /var/log/nginx/error.log \
  && sed -i '/user  nginx;/d' /etc/nginx/nginx.conf \
  && sed -i 's,/var/run/nginx.pid,/tmp/nginx.pid,' /etc/nginx/nginx.conf \
  && sed -i "/^http {/a \    proxy_temp_path /tmp/proxy_temp;\n    client_body_temp_path /tmp/client_temp;\n    fastcgi_temp_path /tmp/fastcgi_temp;\n    uwsgi_temp_path /tmp/uwsgi_temp;\n    scgi_temp_path /tmp/scgi_temp;\n" /etc/nginx/nginx.conf \
  && chown -R 101:0 /usr/share/nginx /var/log/nginx /var/cache/nginx /etc/nginx /var/run \
  && chmod -R g+w /var/cache/nginx \
  && chmod -R g+w /etc/nginx

ADD config /etc/confd

COPY run.sh /run.sh

CMD ["sh", "/run.sh"]
