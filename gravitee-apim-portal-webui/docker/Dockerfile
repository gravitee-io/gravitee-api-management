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

FROM graviteeio/nginx-noconfd:1.27.1
LABEL maintainer="contact@graviteesource.com"

EXPOSE 8080

ENV ALLOWED_FRAME_ANCESTOR_URLS "'self'"
ENV PORTAL_BASE_HREF "/"
ENV PORTAL_API_URL "http://localhost:8083/portal"

COPY ./dist /usr/share/nginx/html/

COPY docker/config/config.json /usr/share/nginx/html/assets/config.json
COPY docker/config/config-next.json /usr/share/nginx/html/next/browser/assets/config.json
COPY docker/config/default.conf /etc/nginx/conf.d/default.conf

RUN chown -R 101:0 /usr/share/nginx/ /etc/nginx/

COPY docker/config/portal-next-run.sh /portal-next-run.sh

CMD ["/bin/sh", "-c", "sh /portal-next-run.sh; sh /run.sh"]

USER nginx

