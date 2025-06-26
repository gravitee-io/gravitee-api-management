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
FROM graviteeio/java:21 AS base
ENV GRAVITEEIO_HOME=/opt/graviteeio-gateway

RUN apk update  \
    && apk add --no-cache libc6-compat jemalloc \
    && if [ $(apk --print-arch) = "aarch64" ]; then ln -s /lib/libc.musl-aarch64.so.1 /lib/ld-linux-aarch64.so.2; fi


RUN addgroup -g 1000 graviteeio \
    && adduser -D -H -u 1001 graviteeio --ingroup graviteeio

# Second stage to add the folder and change the permissions and ownership
FROM base AS builder
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
USER graviteeio
