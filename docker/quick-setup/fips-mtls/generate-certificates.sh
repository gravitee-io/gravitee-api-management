#!/usr/bin/env bash
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

set -e
cd "$(dirname "$0")"
mkdir -p .certificates

# Server certificate for the gateway TLS listener. PEM, because under BC-FIPS approved-only
# it is the only keystore format that loads.
openssl req -x509 -newkey rsa:2048 -nodes -days 365 \
  -keyout .certificates/server.key -out .certificates/server.crt \
  -subj "/CN=localhost" -addext "subjectAltName=DNS:localhost,DNS:gateway,IP:127.0.0.1"

# Client leaf presented by the caller and registered on the application. CA:FALSE is required:
# APIM rejects a certificate authority as an application client certificate.
openssl req -x509 -newkey rsa:2048 -nodes -days 365 \
  -keyout .certificates/client.key -out .certificates/client.crt \
  -subj "/CN=mtls-test-client" \
  -addext "basicConstraints=critical,CA:FALSE" \
  -addext "keyUsage=critical,digitalSignature,keyEncipherment" \
  -addext "extendedKeyUsage=clientAuth"

# openssl writes keys 0600, and the gateway container runs as the unprivileged graviteeio
# user, which could not read them through the bind mount. Throwaway test material.
chmod 644 .certificates/*.key

echo "Certificates written to .certificates/"
