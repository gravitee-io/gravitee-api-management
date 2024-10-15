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

ENV CONSOLE_BASE_HREF "/"
ENV MGMT_API_URL "http://localhost:8083/management"

COPY ./dist /usr/share/nginx/html/

ADD docker/config/constants.json /usr/share/nginx/html/constants.json
ADD docker/config/default.conf /etc/nginx/conf.d/default.conf

RUN chown -R 101:0 /usr/share/nginx/ /etc/nginx/

CMD ["sh", "/run.sh"]

USER nginx

