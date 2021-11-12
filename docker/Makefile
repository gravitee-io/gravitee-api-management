# -----------------------------------------------------------------------------
#                              CUSTOM FUNCTION
# -----------------------------------------------------------------------------

# -----------------------------------------------------------------------------
#                                ENV VARIABLE
# -----------------------------------------------------------------------------

# API Management version for docker images pulling
APIM_VERSION:=nightly
AE_VERSION:=1.4.0

# Retrieve current file name, must be done before doing "include .env" ...
makefile := $(MAKEFILE_LIST)
# Projects list (extracted from .env file, looking for every XXX_REPOSITORY variables)

# -----------------------------------------------------------------------------
#                                 Main targets
# -----------------------------------------------------------------------------

help: ## Print this message
	@echo "\033[0;36m Will run by default with 'nightly' images, but you can pass version with: \033[0mmake \033[0;33m{target}\033[0m \033[0mAPIM_VERSION=\033[0;33m{version} \033[0mAE_VERSION=\033[0;33m{version} \036"
	@echo "\033[0;36m Example: \033[0mmake \033[0;33mtags-internal-external\033[0m \033[0mAPIM_VERSION=\033[0;33m3.10.0 \036"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(makefile) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-35s\033[0m %s\n", $$1, $$2}'


gateway-http-bridge-repository: ## Run API Management with two gateways as Bridge HTTP Server and Bridge HTTP Client.
	cd ./quick-setup/gateway-http-bridge-repository && docker-compose down -v && docker-compose pull && docker-compose up -d

tags-internal-external: ## Run API Management with two gateways configured with internal and external tag.
	cd ./quick-setup/tags-internal-external && docker-compose down -v && docker-compose pull && docker-compose up -d

redis-rate-limit: ## Run API Management with redis for rate limiting.
	cd ./quick-setup/redis-rate-limit && docker-compose down -v && docker-compose pull && docker-compose up -d

opentracing-jaeger: ## Run API Management with opentracing service activated and jaeger as a tracer.
	cd ./quick-setup/opentracing-jaeger && docker-compose down -v && docker-compose pull && docker-compose up -d

prometheus: ## Run API Management with Prometheus support enabled.
	cd ./quick-setup/prometheus && docker-compose down -v && docker-compose pull && docker-compose up -d

https-gateway: ## Run API Management with HTTPS enabled.
	cd ./quick-setup/https-gateway && docker-compose down -v && docker-compose pull && docker-compose up -d

mongodb: ## Run API Management with MongoDB as database.
	cd ./quick-setup/mongodb && docker-compose down -v && docker-compose pull && docker-compose up -d

postgresql: ## Run API Management with PostgreSQL as database.
	cd ./quick-setup/postgresql && docker-compose down -v && docker-compose pull && docker-compose up -d

keycloak: ## Run API Management with KeyCloak.
	cd ./quick-setup/keycloak && docker-compose down -v && docker-compose pull && docker-compose up -d

ee-with-alert-engine: ## Run API Management Enterprise Edition with Alert Engine. ⚠️ No nightly image for this for the moment. Do not forget to suffix version with '-ee'.
	cd ./quick-setup/ee-with-alert-engine && docker-compose down -v && docker-compose pull && docker-compose up -d

nginx: ## Run API Management with an nginx. ⚠️ No nightly image for this for the moment. Do not forget to suffix version with '-ee'.
	cd ./quick-setup/nginx && docker-compose down -v && docker-compose pull && docker-compose up -d

opensearch: ## Run API Management with OpenSearch.
	cd ./quick-setup/opensearch && docker-compose down -v && docker-compose pull && docker-compose up -d

start: ## Start a docker-compose based on target. Example: make start TARGET=tags-internal-external. You can use the optional SERVICES to provide the service to start. Example: make start TARGET=tags-internal-external SERVICES=elasticsearch
ifeq ($(TARGET),)
	@echo "\033[0;31m ⚠️ Please provide the target you want to start \033[0m"
	@echo " 👍 Example: make start TARGET=tags-internal-external"
else
	cd ./quick-setup/$(TARGET) && docker-compose start $(SERVICES)
endif

stop: ## Stop a docker-compose based on target. Example: make stop TARGET=tags-internal-external. You can use the optional SERVICES to provide the service to stop. Example: make stop TARGET=tags-internal-external SERVICES=elasticsearch
ifeq ($(TARGET),)
	@echo "\033[0;31m ⚠️ Please provide the target you want to stop \033[0m"
	@echo " 👍 Example: make stop TARGET=tags-internal-external"
else
	cd ./quick-setup/$(TARGET) && docker-compose stop $(SERVICES)
endif

down: ## Down a docker-compose based on target. Example: make down TARGET=tags-internal-external.
ifeq ($(TARGET),)
	@echo "\033[0;31m ⚠️ Please provide the target you want to down \033[0m"
	@echo " 👍 Example: make down TARGET=tags-internal-external"
else
	cd ./quick-setup/$(TARGET) && docker-compose down
endif

.DEFAULT_GOAL := help
.PHONY: http-bridge, tags-internal-external, redis-rate-limit, opentracing, prometheus, https-gateway, mongodb, keycloak, ee-with-alert-engine
