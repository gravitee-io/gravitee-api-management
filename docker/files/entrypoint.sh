#!/bin/bash

setup() {
    echo "Configure management api url to ${MGMT_API_URL}"
    cat /var/www/html/constants.json.template | \
    sed "s#http://localhost:8083/management/#${MGMT_API_URL}#g" > /var/www/html/constants.json
}

setup
exec "$@"