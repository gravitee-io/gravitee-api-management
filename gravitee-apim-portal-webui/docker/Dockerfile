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

FROM graviteeio/nginx:1.27.5
LABEL maintainer="contact@graviteesource.com"

EXPOSE 8080

ENV ALLOWED_FRAME_ANCESTOR_URLS="'self'"
ENV PORTAL_BASE_HREF="/"
ENV PORTAL_API_URL="http://localhost:8083/portal"
ENV DEFAULT_PORTAL="classic"

COPY ./dist /usr/share/nginx/html/

COPY docker/config/config.json /usr/share/nginx/html/assets/config.json
COPY docker/config/config-next.json /usr/share/nginx/html/next/browser/assets/config.json
COPY docker/config/default.conf /etc/nginx/conf.d/default.conf
COPY docker/config/default.no-ipv6.conf /etc/nginx/conf.d/default.no-ipv6.conf
COPY docker/config/default-next.conf /etc/nginx/conf.d/default-next.conf
COPY docker/config/default-next.no-ipv6.conf /etc/nginx/conf.d/default-next.no-ipv6.conf

RUN chown -R nginx:root /usr/share/nginx /etc/nginx \
    && chmod -R g=u /usr/share/nginx /etc/nginx

COPY --chown=nginx:root docker/config/portal-next-run.sh /
COPY --chown=nginx:root docker/config/check_ip_config.sh /

USER nginx
CMD ["/bin/sh", "-c", "sh /portal-next-run.sh; sh /check_ip_config.sh; sh /run.sh"]
