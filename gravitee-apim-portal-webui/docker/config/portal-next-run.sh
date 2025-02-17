#!/bin/sh
#
# Copyright (C) 2015-2022 The Gravitee team (http://gravitee.io)
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

# generate configs
if [ -f "/usr/share/nginx/html/next/browser/assets/config.json" ]; then
    envsubst < /usr/share/nginx/html/next/browser/assets/config.json > /usr/share/nginx/html/next/browser/assets/config.json.tmp
    mv /usr/share/nginx/html/next/browser/assets/config.json.tmp /usr/share/nginx/html/next/browser/assets/config.json
fi

if [ "$DEFAULT_PORTAL" = "next" ]; then
    cp /etc/nginx/conf.d/default-next.conf /etc/nginx/conf.d/default.conf
fi
