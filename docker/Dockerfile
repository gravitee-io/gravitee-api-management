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
FROM nginx:1.16-alpine
MAINTAINER Gravitee Team <http://gravitee.io>

ARG GRAVITEEIO_VERSION=0

ENV CONFD_VERSION="0.16.0"
ENV CONFD_URL="https://github.com/kelseyhightower/confd/releases/download"

# Update to get support for Zip/Unzip, Bash
RUN apk --update add zip unzip bash wget

ENV WWW_TARGET /var/www/html/

RUN wget https://download.gravitee.io/graviteeio-apim/components/gravitee-management-webui/gravitee-management-webui-${GRAVITEEIO_VERSION}.zip --no-check-certificate -P /tmp/ \
    && unzip /tmp/gravitee-management-webui-${GRAVITEEIO_VERSION}.zip -d /tmp/ \
    && mkdir -p ${WWW_TARGET} \
    && mv /tmp/gravitee-management-webui-${GRAVITEEIO_VERSION}/* ${WWW_TARGET} \
    && rm -rf /tmp/* \
    && wget -T 5 ${CONFD_URL}/v${CONFD_VERSION}/confd-${CONFD_VERSION}-linux-amd64 -O /bin/confd \
    && chmod +x /bin/confd

# support running as arbitrary user which belogs to the root group
RUN chgrp -R 0 /var/www/ /var/log/nginx /var/cache/nginx/ /etc/nginx/ /var/run/ && \
    chmod -R g=u /var/www/ /var/log/nginx /var/cache/nginx/ /etc/nginx/ /var/run/

ADD config /etc/confd

COPY run.sh /run.sh

CMD ["sh", "/run.sh"]
