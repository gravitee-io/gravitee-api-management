#!/bin/bash

setup() {
    echo "Configure portal api url to ${PORTAL_API_URL}"
    cat /var/www/html/assets/config.json.template | \
    sed "s#/portal/environments/DEFAULT#${PORTAL_API_URL}#g" > /var/www/html/assets/config.json
}

setup
exec "$@"
