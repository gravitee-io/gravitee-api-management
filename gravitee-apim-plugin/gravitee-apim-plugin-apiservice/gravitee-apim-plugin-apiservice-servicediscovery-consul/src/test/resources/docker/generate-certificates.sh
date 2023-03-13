#!/usr/bin/env bash
#set -e

CONFIG_SSL_PATH="config/ssl"

echo
echo "Welcome to the Consul SSL keystore and truststore generator script."

echo
echo -n "First, do you need to generate truststore and consul server certificate [yn] "
read generate_trust_store

if [ "$generate_trust_store" == "y" ]; then

    echo
    echo "Generating certificate authority with truststore"

    # Create a certificate authority.
    openssl req -newkey rsa:4096 -keyform PEM -keyout $CONFIG_SSL_PATH/ca.key -x509 -days 3650 -subj "/emailAddress=contact@graviteesource.com/CN=consul-ca/OU=APIM/O=GraviteeSource/L=Lille/ST=France/C=FR" -passout pass:gravitee -outform PEM -out $CONFIG_SSL_PATH/ca.pem

    echo
    echo "Certificate authority generated!"

    # Consul server key (host: server.dc1.consul san: localhost)
    echo
    echo "Generating Consul server certificate"

    openssl genrsa -out $CONFIG_SSL_PATH/server1.dc1.consul.key 4096
    openssl req -new -key $CONFIG_SSL_PATH/server1.dc1.consul.key -out $CONFIG_SSL_PATH/server1.dc1.consul.csr -sha256 -subj "/CN=server.dc1.consul"

    echo "[ req ]
    distinguished_name = req_dn
    req_extensions = req_ext

    [req_dn]
    CN = *.dc1.consul

    [ req_ext ]
    basicConstraints=CA:FALSE
    subjectAltName = @alt_names

    [alt_names]
    DNS.1 = server.dc1.consul
    DNS.2 = localhost
    IP.1  = 127.0.0.1" > $CONFIG_SSL_PATH/server1.dc1.consul.cnf

    openssl x509 -req -in $CONFIG_SSL_PATH/server1.dc1.consul.csr -CA $CONFIG_SSL_PATH/ca.pem -CAkey $CONFIG_SSL_PATH/ca.key -set_serial 100 -days 1460 -outform PEM -out $CONFIG_SSL_PATH/server1.dc1.consul.crt -sha256 -passin pass:gravitee -extensions req_ext -extfile $CONFIG_SSL_PATH/server1.dc1.consul.cnf
    rm $CONFIG_SSL_PATH/server1.dc1.consul.cnf

    echo
    echo "Consul server certificate generated!"
fi

echo
echo -n "Do you need to generate a client certificate to use for MTLS scenario [yn] "
read generate_client_cert

if [ "$generate_client_cert" == "y" ]; then
    echo
    echo "Generating client certificate"

    # Keystore
    openssl genrsa -out $CONFIG_SSL_PATH/client.key 4096
    openssl req -new -key $CONFIG_SSL_PATH/client.key -out $CONFIG_SSL_PATH/client.csr -subj "/CN=client.dc1.consul"
    openssl x509 -req -in $CONFIG_SSL_PATH/client.csr -CA $CONFIG_SSL_PATH/ca.pem -CAkey $CONFIG_SSL_PATH/ca.key -set_serial 101 -extensions client -days 365 -outform PEM -out $CONFIG_SSL_PATH/client.cer -passin pass:gravitee

    openssl pkcs12 -export -inkey $CONFIG_SSL_PATH/client.key -in $CONFIG_SSL_PATH/client.cer -out $CONFIG_SSL_PATH/client-keystore.p12 -passout pass:gravitee -name consul-client
    keytool -importkeystore -srckeystore $CONFIG_SSL_PATH/client-keystore.p12 -destkeystore $CONFIG_SSL_PATH/client-keystore.jks -srcstoretype PKCS12 -deststoretype JKS -srcstorepass gravitee -deststorepass gravitee

    # Truststore
    # Add the ca.
    keytool -import -file $CONFIG_SSL_PATH/ca.pem -storetype JKS -keystore $CONFIG_SSL_PATH/client-truststore.jks -storepass gravitee -noprompt -alias consul-ca

    # Add the server cert
    keytool -import -file $CONFIG_SSL_PATH/server1.dc1.consul.crt -storetype JKS -keystore $CONFIG_SSL_PATH/client-truststore.jks -storepass gravitee -noprompt -alias consul-server

    # Do some useful conversions.
    keytool -importkeystore -srckeystore $CONFIG_SSL_PATH/client-truststore.jks -srcstorepass gravitee -destkeystore $CONFIG_SSL_PATH/client-truststore.p12 -storepass gravitee -deststoretype pkcs12
    openssl pkcs12 -nodes -in $CONFIG_SSL_PATH/client-truststore.p12 -out $CONFIG_SSL_PATH/client-truststore.pem -passin pass:gravitee

    echo
    echo "Client certificate generated!"
fi
