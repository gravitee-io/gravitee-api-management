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

CONFIG_SSL_PATH=".certificates"

SERVER_KEYSTORE_P12="$CONFIG_SSL_PATH/server.keystore.p12"
SERVER_TRUSTSTORE_P12="$CONFIG_SSL_PATH/server.truststore.p12"

CLIENT_KEYSTORE_P12="$CONFIG_SSL_PATH/client.keystore.p12"
CLIENT_TRUSTSTORE_P12="$CONFIG_SSL_PATH/client.truststore.p12"
CLIENT_TRUSTSTORE_PEM="$CONFIG_SSL_PATH/client.truststore.pem"

echo
echo "Generating certificate authority with truststore"

# Create a certificate authority.
openssl req -newkey rsa:4096 -keyform PEM -keyout $CONFIG_SSL_PATH/ca.key -x509 -days 3650 -subj "/emailAddress=contact@graviteesource.com/CN=gio-ca/OU=Demo/O=GraviteeSource/L=Lille/ST=France/C=FR" -passout pass:gravitee -outform PEM -out $CONFIG_SSL_PATH/ca.pem
keytool -import -file $CONFIG_SSL_PATH/ca.pem -storetype PKCS12 -keystore $SERVER_TRUSTSTORE_P12 -storepass gravitee -noprompt -alias gravitee-ca
keytool -import -file $CONFIG_SSL_PATH/ca.pem -storetype PKCS12 -keystore $CLIENT_TRUSTSTORE_P12 -storepass gravitee -noprompt -alias gravitee-ca

echo
echo "Certificate authority and truststore generated!"
#
# server key (host: gravitee san: localhost)
echo
echo "Generating server certificate"

openssl genrsa -out $CONFIG_SSL_PATH/server.key 4096
openssl req -new -key $CONFIG_SSL_PATH/server.key -out $CONFIG_SSL_PATH/server.csr -sha256 -subj "/emailAddress=contact@graviteesource.com/CN=gio-gateway/OU=Demo/O=GraviteeSource/L=Lille/ST=France/C=FR"

echo "[ req ]
distinguished_name = req_distinguished_name
req_extensions = v3_req
extensions = server
prompt = no

[req_distinguished_name]
emailAddress = contact@graviteesource.com
CN = gravitee
OU = APIM
O = GraviteeSource
L = Lille
ST = France

[ v3_req ]
subjectAltName = @alt_names
[alt_names]
DNS.1 = gravitee
DNS.2 = localhost" > $CONFIG_SSL_PATH/server.cnf

openssl x509 -req -in $CONFIG_SSL_PATH/server.csr -CA $CONFIG_SSL_PATH/ca.pem -CAkey $CONFIG_SSL_PATH/ca.key -set_serial 100 -days 1460 -outform PEM -out $CONFIG_SSL_PATH/server.cer -sha256 -passin pass:gravitee -extensions v3_req -extfile $CONFIG_SSL_PATH/server.cnf
openssl pkcs12 -export -inkey $CONFIG_SSL_PATH/server.key -in $CONFIG_SSL_PATH/server.cer -out $SERVER_KEYSTORE_P12 -passout pass:gravitee -name server
rm $CONFIG_SSL_PATH/server.cnf

echo
echo "server certificate generated!"

echo
echo "Generating NGINX certificate"

openssl genrsa -out $CONFIG_SSL_PATH/nginx.key 4096
openssl req -new -key $CONFIG_SSL_PATH/nginx.key -out $CONFIG_SSL_PATH/nginx.csr -sha256 -subj "/emailAddress=nginx@graviteesource.com/CN=nginx/OU=Demo/O=GraviteeSource/L=Lille/ST=France/C=FR"

echo "[ req ]
distinguished_name = req_distinguished_name
req_extensions = v3_req
extensions = server
prompt = no

[req_distinguished_name]
emailAddress = nginx@graviteesource.com
CN = nginx
OU = APIM
O = GraviteeSource
L = Lille
ST = France

[ v3_req ]
subjectAltName = @alt_names
[alt_names]
DNS.1 = nginx
DNS.2 = localhost" > $CONFIG_SSL_PATH/nginx.cnf

openssl x509 -req -in $CONFIG_SSL_PATH/nginx.csr -CA $CONFIG_SSL_PATH/ca.pem -CAkey $CONFIG_SSL_PATH/ca.key -set_serial 100 -days 1460 -outform PEM -out $CONFIG_SSL_PATH/nginx.cer -sha256 -passin pass:gravitee -extensions v3_req -extfile $CONFIG_SSL_PATH/nginx.cnf
openssl pkcs12 -export -inkey $CONFIG_SSL_PATH/nginx.key -in $CONFIG_SSL_PATH/nginx.cer -out $SERVER_KEYSTORE_P12 -passout pass:gravitee -name nginx
rm $CONFIG_SSL_PATH/nginx.cnf


echo
echo "server NGINX generated!"

echo
echo "Generating client certificate"

# Client key
openssl genrsa -out $CONFIG_SSL_PATH/client.key 4096
openssl req -new -key $CONFIG_SSL_PATH/client.key -out $CONFIG_SSL_PATH/client.csr -subj "/emailAddress=client@graviteesource.com/CN=localhost/OU=Demo/O=GraviteeSource/L=Lille/ST=France/C=FR"
openssl x509 -req -in $CONFIG_SSL_PATH/client.csr -CA $CONFIG_SSL_PATH/ca.pem -CAkey $CONFIG_SSL_PATH/ca.key -set_serial 101 -days 365 -outform PEM -out $CONFIG_SSL_PATH/client.cer -passin pass:gravitee
openssl pkcs12 -export -inkey $CONFIG_SSL_PATH/client.key -in $CONFIG_SSL_PATH/client.cer -out $CLIENT_KEYSTORE_P12 -passout pass:gravitee -name client

echo
echo "Client certificate generated!"
