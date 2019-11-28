#!/bin/bash

function gravitee_web_init() {
  {
    docker build -f dockerfiles/base.Dockerfile -t gravitee-portal-webui/base --compress --force-rm .
  } || {
    docker rmi gravitee-portal-webui/base
    docker image prune -f
    exit 1
  }
}

function gravitee_web_deps() {
  {
    docker build --rm -f dockerfiles/dependencies.Dockerfile -t gravitee-portal-webui/dependencies --compress --force-rm --no-cache .
  } || {
    docker rmi gravitee-portal-webui/dependencies
    docker image prune -f
    exit 1
  }
}

function gravitee_web_test() {
  {
    docker build --rm -f dockerfiles/test.Dockerfile -t gravitee-portal-webui/test --compress --force-rm --no-cache .
    docker run --rm -v `pwd`"/coverage:/usr/target/coverage" gravitee-portal-webui/test
    docker rmi gravitee-portal-webui/test
  } || {
    docker rmi gravitee-portal-webui/test
    docker image prune -f
    exit 1
  }
}

function gravitee_web_package() {
  {
    docker build --rm -f dockerfiles/package.Dockerfile -t gravitee-portal-webui/package --compress --force-rm --no-cache .
    docker run --rm -v `pwd`"/dist:/usr/target/dist" gravitee-portal-webui/package
    gravitee_web_clean
  } || {
    gravitee_web_clean
    docker image prune -f
    exit 1
  }
}

function gravitee_web_clean() {
   docker rmi gravitee-portal-webui/package || true
   docker rmi gravitee-portal-webui/test || true
   docker rmi gravitee-portal-webui/dependencies || true
}

case $1 in
  "gravitee_web_clean")
    gravitee_web_clean;;
  "gravitee_web_init")
    gravitee_web_init;;
  "gravitee_web_deps")
    gravitee_web_deps;;
  "gravitee_web_test")
    gravitee_web_test;;
  "gravitee_web_package")
    gravitee_web_package;;
  *) echo "Sorry, use ./build.sh gravitee_web_[clean|init|deps|test|package]"
esac
