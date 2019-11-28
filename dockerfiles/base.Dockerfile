FROM node:12.13.1-alpine

RUN set -x \
    && apk update \
    && apk upgrade \
    && apk add --no-cache \
    bash \
    git \
    openssh \
    udev \
    ttf-freefont \
    chromium

ENV CHROME_BIN="/usr/bin/chromium-browser"
