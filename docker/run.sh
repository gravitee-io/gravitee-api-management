#!/bin/sh

# generate configs
/bin/confd -onetime -backend env

# start nginx foreground
exec /usr/sbin/nginx -g 'daemon off;'