# -----------------------------------------------------------------------------
#                              CUSTOM FUNCTION
# -----------------------------------------------------------------------------

# -----------------------------------------------------------------------------
#                                ENV VARIABLE
# -----------------------------------------------------------------------------

# API Management version for docker images pulling
APIM_VERSION:=nightly

# Retrieve current file name, must be done before doing "include .env" ...
makefile := $(MAKEFILE_LIST)
# Projects list (extracted from .env file, looking for every XXX_REPOSITORY variables)

# -----------------------------------------------------------------------------
#                                 Main targets
# -----------------------------------------------------------------------------

help: ## Print this message
	@echo "\033[0;36m Will run by default with 'master-latest' images coming from 'graviteeio.azurecr.io' registry, but you can pass version with: \033[0mmake \033[0;33mall | hivemq \033[0m \033[0mAPIM_VERSION=\033[0;33m{version} \036 APIM_REGISTRY=\033[0;33m{registry} \036"
	@echo "\033[0;36m Example: \033[0mmake \033[0;33mall\033[0m \033[0mAPIM_VERSION=\033[0;33m3.20.0 \033[0mAPIM_REGISTRY=\033[0;33mhub.docker.com \033[0m"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(makefile) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-35s\033[0m %s\n", $$1, $$2}'

all: ## Run API Management with hivemq broker
	cd ./quick-setup/apim-hivemq && docker-compose down -v && docker-compose pull && docker-compose up -d

start-all: ## Start API Management with hivemq broker
	cd ./quick-setup/apim-hivemq && docker-compose start

stop-all: ## Stop API Management with hivemq broker
	cd ./quick-setup/apim-hivemq && docker-compose stop

hivemq: ## Run only the hivemq service
	cd ./quick-setup/apim-hivemq && docker-compose up --force-recreate -d hivemq

start-hivemq: ## Start only the hivemq service
	cd ./quick-setup/apim-hivemq && docker-compose start hivemq

stop-hivemq: ## Stop only the hivemq service
	cd ./quick-setup/apim-hivemq && docker-compose stop hivemq

down: ## Remove all.
	cd ./quick-setup/apim-hivemq && docker-compose down

.DEFAULT_GOAL := help
.PHONY: all, start-all, stop-all, hivemq, start-hivemq, stop-hivemq, down
