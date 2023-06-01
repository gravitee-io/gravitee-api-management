
# Changelog

This file documents all notable changes to [Gravitee.io API Management 3.x](https://github.com/gravitee-io/helm-charts/tree/master/apim/3.x) Helm Chart. The release numbering uses [semantic versioning](http://semver.org).

### 4.0.0

- [X] Change versioning

### 3.2.0

- [X] Move Probes configuration under `deployment:`
- [X] Change probe default configuration
- [X] Remove `apiSync` parameter under `gateway.readinessProbe`
- [X] Allow users to define their own `customStartupProbe` `customReadinessProbe` `customlivenessProbe`
- [X] Allow disabling analytics in Management API
- [X] Update default version of MongoDB in values. Set it to 6.0.6

- **BREAKING CHANGE**: Probes configuration has been changed. Check the default `values.yaml` before upgrade!

### 3.1.68

- [X] Revert Previous commit "Remove old and unused `cache.type` from gateway configmap"

### 3.1.67

- [X] Remove old and unused `cache.type` from gateway config map

### 3.1.66

- [X] Add sni to the gateway configuration

### 3.1.65

- [X] Add hook-delete-policy
- [X] Allow users to use `logging.debug: true` when they define their own configuration file by defining a volume
- [X] Add check to allow automatic Redis plugin download only for versions prior to 3.21.0 (it is now embedded in the distribution)
- [X] Add SSL and Sentinel configuration management for Redis and updated doc. 
- [X] Auto-generate `PORTAL_BASE_HREF` and `CONSOLE_BASE_HREF` environment variables
- [X] Remove ingress nginx annotation when `ingress.class` is not nginx
- [X] Add variables `api.ingress.management.scheme` and `api.ingress.portal.scheme` default `https`

### 3.1.64

- [X] Add `gracefulShutdown` in gateway configuration

### 3.1.63

- [X] Add support of user password policy config on API Management
- [X] Add `externalTrafficPolicy` in service configuration
- [X] Remove `email` block when smtp is disabled

### 3.1.62

- [X] Remove `/gateway` from the gateway ingress path 
- [X] Add `labels` in metrics config of the Management API and Gateway
- [X] Fix default ingress path for ui
- [X] Add API properties config of the Management API and Gateway
- [X] Update APIM version to 3.20.1
- [X] Update Elasticsearch version to 7.17.9
- [X] Update MongoDB version to 5.0.14
- [X] Add variable for ingress path type
- [X] Remove legacy Kube Controller config. This feature is now provided by [Gravitee Operator for Kubernetes](https://github.com/gravitee-io/gravitee-kubernetes-operator)

### 3.1.61

- [X] Fix AE alerts configuration without options
- [X] Add values to define tracing options

### 3.1.60

- [X] Add upgrader framework job
- [X] Add additional Alert Engine configuration
- [X] Add additional logback loggers configuration
- [X] Clean Redis configuration in the `values.yaml` file
- [X] Add documentation for alert engine connector ssl
- [X] Add AE engine list support
- [X] Allow users to define their own configuration file by defining a volume

### 3.1.59

- [X] Replace ClusterRole with Role

### 3.1.58

- [X] Add optional value to use gravitee licence key

### 3.1.57

- [X] Provide advanced support to connect to the Hybrid Bridge

### 3.1.56

- [X] Add support for Openshift Routes by removing ingress annotation
- Update Gravitee.io APIM v3.18.9

### 3.1.55

- [X] Fix: Merge all smtp.properties directly into gravitee.yml
- [X] Fix: Enable notifiers ssl with right smtp.ssl value

- **BREAKING CHANGE**: Use `smtp.properties.starttls.enable` instead of `smtp.properties.starttlsEnable`

### 3.1.54

- [X] Add a startup probe on the Management API
- [X] Truncate port name to respect k8s limit (15 for deployment and 63 for service)

### 3.1.53

- [X] Remove alias for mongodb chart dependency

### 3.1.52

- [X] Use ISO 8601 datetime for apim json logging

### 3.1.51

- [X] Add JSON logging support

### 3.1.50

- [X] Upgrade Mongodb and Elasticsearch dependencies
- [X] Handle subscription service configuration

### 3.1.49

- [X] Add Bridge service to Management API
- [X] Add support for autoscaling/v2
- [X] Add support for appProtocol to the services

- Update Gravitee.io APIM v3.15.10

### 3.1.48

- [X] Add support for ingressClassName

### 3.1.47

- [X] Fix Gateway Service Account for Kube Controller

- Update Gravitee.io APIM v3.15.9

### 3.1.46

- [X] Add support for APIM console Pendo analytics with helm charts

### 3.1.45

- [X] Add support for keystore and truststore in MongoDB configuration
- [X] Add version labels on pods

### 3.1.44

- [X] Add support for managed ServiceAccounts name provided by user

### 3.1.43

- [X] Disable automatic download of Redis plugin

- Update Gravitee.io APIM v3.15.8

### 3.1.42

- [X] Add support for PodDisruptionBudget

### 3.1.41

- [X] Reorder HPA resources to avoid outofsync state with ArgoCD

### 3.1.40

- [X] Fix ignoring the managedServiceAccount in the deployment files

### 3.1.39

- [X] Set default array for topologySpreadConstraints

### 3.1.38

- [X] Add support for topologySpreadConstraints

- Update Gravitee.io APIM v3.15.7

### 3.1.37

- Add template to APIM Cockpit secret name

### 3.1.36

- Fix deploy gateway specific version with default ratelimit
 
### 3.1.35

- Set default entrypoint for Portal Try-it and cURL to the url of the gateway
- Set console `api` and `ui` urls automatically based on ingress values 
- Expose technical API of the management API

### 3.1.34

- Ease the integration of Gravitee.io Cockpit
- Ensure additional jar doesn't exist before downloading it
- Handle JWT attributes in APIM Management API configmap
- Add an option to disable the newsletter popup

- Update Gravitee.io APIM v3.15.2

### 3.1.33

- [X] Disable old classloader to enable the new one by default (since v3.15)
- [X] Add support for kubernetes certificate configuration
- [X] Make app.kubernetes.io/version label consistent
- [X] Add quotes to version to fix #6450
- Update gravitee.io APIM v3.15.1

### 3.1.32

- [X] Update gravitee.io APIM v3.11.3

### 3.1.31

- [X] Add support for ILM managed indexes
- [X] Remove the empty override of 'if-match' header

### 3.1.30 

- [X] Add support for startupProbe

### 3.1.29

- [X] Customization of the readinessProbe

### 3.1.28

- [X] Support Ingress kubernetes >= 1.22.x
- Update gravitee.io APIM v3.11.2

### 3.1.27

- [X] Manage redis repository plugin for nightly tag

### 3.1.25

- [X] Extended configuration for init containers
- [X] Support proxy at hybrid gateway level and system level

### 3.1.24

- [X] Fix Management UI URL

### 3.1.23

- [X] Fix init containers image repository and tag

### 3.1.22

- [X] Configure init containers image repository and tag
- Update gravitee.io APIM v3.11.1

### 3.1.21

- [X] Configure gateway sync service from ConfigMap

### 3.1.20

- [X] Configure deployment strategy

### 3.1.19

- [X] Provide a way to remove default plugins

### 3.1.18

- [X] Fix alert-engine WS connection
- [X] Configure ES timeout
- [X] Fix typos in ES reporter lifecycle definitions
- Update gravitee.io APIM v3.10.0

### 3.1.17

- [X] Fix issues with HTTP Bridge configuration

### 3.1.16

- [X] Fix typo in API configmap

### 3.1.15

- [X] Add support for tolerations for Portal

### 3.1.14

- [X] Improve Deployment configurability

### 3.1.13

- [X] Gateway Bridge Ingress based on networking.k8s.io

### 3.1.12

- [X] Allow to configure Prometheus support for API Gateway and Management API
- Update gravitee.io APIM v3.8.4

### 3.1.11

- [X] Allow to use the management node API as readinessProbe

### 3.1.10

- [X] Fix Alert Engine configuration when alerting is disabled

### 3.1.9

- [X] Rename Alert Engine connector when alerting is disabled

### 3.1.8

- [X] Add support JDBC connection pool
- [X] Fix baseURL generation issue
- [X] Correctly evaluate ingress properties to customize constants.json file
- [X] Enable only if the ssl dictionary is defined from values
- [X] Downgrade required kube version to 1.14
- Update gravitee.io APIM v3.8.3

### 3.1.7

- [X] Create an init container with JDBC driver for management API when JDBC is enabled

### 3.1.6

- [X] Add support for policy configuration from gravitee.yml

### 3.1.5

- [X] Client authentication support for HTTP bridge (hybrid deployment)

### 3.1.4

- [X] [#94](https://github.com/gravitee-io/helm-charts/pull/94) Do not apply special treatment for nightly tag
- Update gravitee.io APIM v3.7.0

### 3.1.3

- [X] Disable HTTP proxy by default

### 3.1.2

- [X] Addition of configuration values for the kubernetes controller plugins
- [X] Add configuration for Elasticsearch ILM
- [X] Map handlers for API Gateway to override X-Transaction and X-Request
- [X] Portal extraInitContainers mapped to ui

### 3.1.1

- [X] Add support for extended reporters configurations
- [X] Allow to configure trustAll for identity providers

### 3.1.0

- [X] Manage default plugins
- Update gravitee.io APIM v3.6.0

### 3.0.21

- [X] Add authSource field to ratelimit section
- Update gravitee.io APIM v3.5.3

### 3.0.20

- [X] Allows to deploy nightly version of APIM
- [X] Add mongo authSource field to configmaps when not using Mongo URI

### 3.0.19

- [X] Externalize securityContext from deployments

### 3.0.18

- [X] Enable technical api for api and gateway by default
- [X] Add sane default value for technical api password
- [X] Add gateway technical api spec to values.yaml
- [X] Add technical ingress to gateway, like what was done for api-component
- [X] Add technical api spec to gateway service
- [X] Parametrize technical api portion of gateway configmap
- [X] Add technical api enable flag and auth type options to api-configmap
- [X] Update ui-deployment.yaml
- [X] Minor ingress improvements 

### 3.0.17

- Update gravitee.io APIM v3.5.2

### 3.0.16

- Update gravitee.io APIM v3.5.0

### 3.0.15

- [X] Fix mongo env var overloading using helm
- [X] Add default company name to values.yaml
- [X] Add company name to template
- [X] Update Helm dependencies repository
- [X] Allow to configure the fullname of services, deployments, ...
- [X] Add condition for replica setting
- [X] Simplify configuration of jdbc driver and other plugins

### 3.0.14

- [X] Add custom management console URL handling resolve

- Update gravitee.io APIM v3.4.0

### 3.0.13

- [X] Allow the configuration of an OIDC provider
- [X] Fix helm requirements
- [X] Add support for sidecar containers
- [X] Add APIM redis example + remove values examples

### 3.0.12

- [X] Add support for HTTP bridge deployment
- [X] Fix issue with smtp templating values

- Update gravitee.io APIM v3.3.3
