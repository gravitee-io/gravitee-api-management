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
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(makefile) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'


http-bridge: ## Run API Management with two gateways as Bridge HTTP Server and Bridge HTTP Client. Will run by default with 'nightly' images, but you can pass version with: make http-bridge APIM_VERSION={version}
	cd ./quick-setup/gateway-http-bridge-repository && docker-compose down -v && docker-compose pull && docker-compose up

tags-internal-external: ## Run API Management with two gateways configured with internal and external tag. Will run by default with 'nightly' images, but you can pass version with: make tags-internal-external APIM_VERSION={version}
	cd ./quick-setup/tags-internal-external && docker-compose down -v && docker-compose pull && docker-compose up

redis-rate-limit: ## Run API Management with redis for rate limiting. Will run by default with 'nightly' images, but you can pass version with: make redis-rate-limit APIM_VERSION={version}
	cd ./quick-setup/redis-rate-limit && docker-compose down -v && docker-compose pull && docker-compose up

opentracing: ## Run API Management with opentracing service activated and jaeger as a tracer. Will run by default with 'nightly' images, but you can pass version with: make opentracing APIM_VERSION={version}
	cd ./quick-setup/opentracing-jaeger && docker-compose down -v && docker-compose pull && docker-compose up

prometheus: ## Run API Management with Prometheus support enabled. Will run by default with 'nightly' images, but you can pass version with: make prometheus APIM_VERSION={version}
	cd ./quick-setup/prometheus && docker-compose down -v && docker-compose pull && docker-compose up

.DEFAULT_GOAL := help
.PHONY: http-bridge, tags-internal-external, redis-rate-limit, opentracing
