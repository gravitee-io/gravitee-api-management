#
# Copyright © 2015 The Gravitee team (http://gravitee.io)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# First stage to share environment variable
FROM graviteeio/java:17-alpine-3.20 as base
ENV GRAVITEEIO_HOME /opt/graviteeio-gateway

RUN apk update  \
    && apk add --no-cache libc6-compat  \
    && apkArch="$(apk --print-arch)" \
    && ln -s /lib/libc.musl-${apkArch}.so.1 /lib/ld-linux-${apkArch/_/-}.so.2

RUN addgroup -g 1000 graviteeio \
    && adduser -D -H -u 1001 graviteeio --ingroup graviteeio

# Second stage to add the folder and change the permissions and ownership
FROM base as builder
ADD ./distribution ${GRAVITEEIO_HOME}/

RUN chgrp -R graviteeio ${GRAVITEEIO_HOME} && \
    chmod -R g=u ${GRAVITEEIO_HOME}

# Third stage to build the final docker image. COPY preserves ownership & permissions
FROM base
LABEL maintainer="contact@graviteesource.com"
COPY --from=builder ${GRAVITEEIO_HOME} ${GRAVITEEIO_HOME}
WORKDIR ${GRAVITEEIO_HOME}
EXPOSE 8082
ENTRYPOINT ["./bin/gravitee"]
VOLUME ${GRAVITEEIO_HOME}/logs
HEALTHCHECK --interval=5s --timeout=3s --retries=6 --start-period=30s \
            CMD curl --user admin:adminadmin --fail http://localhost:18082/_node/health || exit 1
USER graviteeio
