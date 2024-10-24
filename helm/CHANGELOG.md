
# Changelog

This file documents all notable changes to [Gravitee.io API Management 3.x](https://github.com/gravitee-io/helm-charts/tree/master/apim/3.x) Helm Chart. The release numbering uses [semantic versioning](http://semver.org).

### 4.3.16

- add missing haproxy mapping attribute
- fix: add heartbeat values and entries in helm charts

### 4.3.15

- fix: handle annotations for non nginx ingress
- fix: add missing service account for portal and ui

### 4.3.14

- fix: add missing keystore secret configuration in helm chart

### 4.3.13

- fix(helm): add missing common.labels in gateway-technical-ingress.yaml
- feat(helm): improve the probes definition for the gateway

### 4.3.11

- fix: add redis rate limit operation timeout, tcp connectTimeout and idleTimeout in helm charts

### 4.3.9

- fix: allow users to define multiple DNS for the Management API
- fix: avoid condition nil exception on helm api console and portal url values

### 4.3.5

- BREAKING CHANGE: In gateway ingress controller, change ssl-redirect option from "false" to default. More info [here](https://kubernetes.github.io/ingress-nginx/user-guide/nginx-configuration/annotations/#server-side-https-enforcement-through-redirect)

### 4.3.4

- Improve redis ratelimit configuration [issues/9726](https://github.com/gravitee-io/issues/issues/9726). Thanks [@gh0stsrc](https://github.com/gh0stsrc)

### 4.3.0

- Added "gateway.services.core.http.ssl.keystore.password"
- fix helm backward compatibility during helm upgrade without `common` field
- Added default preStop command on ui and portal
- Add networkPolicy
- Update regex for portal and console base_href
- 'fix AE system mail notification without keystore'
- Add support for Secret Manager's configuration
- Add networkPolicy
- fix helm backward compatibility during helm upgrade without `common` field
- BREAKING CHANGE: deprecated api|gateway|ui|portal.securityContext has been removed

### 4.1.15

- Improve redis ratelimit configuration [issues/9726](https://github.com/gravitee-io/issues/issues/9726). Thanks [@gh0stsrc](https://github.com/gh0stsrc)

### 4.1.4

- "fix 'gravitee.yml' > 'services.metrics' definition from helm `values.yaml`"
- Add requestTimeout and requestTimeoutGraceDelay in gateway

### 4.1.0

- Avoid empty user when disabling admin user
- Add revision history limit on portal
- Add podSecurityContext
- Add support for DB less mode on gateway
- Add nodePort value to all services
- Remove smtp default example values
- Allow wildcard in ingress host
- Add unknownExpireAfter in management-api configuration

### 4.0.24

- Improve redis ratelimit configuration [issues/9726](https://github.com/gravitee-io/issues/issues/9726). Thanks [@gh0stsrc](https://github.com/gh0stsrc)

### 4.0.13

- "fix 'gravitee.yml' > 'services.metrics' definition from helm `values.yaml`"
- Add requestTimeout and requestTimeoutGraceDelay in gateway

### 4.0.9

- Allow wildcard in ingress host
- Remove smtp default example values
- Add unknownExpireAfter in management-api configuration

### 4.0.6

- Add revision history limit on portal
- Add podSecurityContext
- Avoid empty user when disabling admin user


### 4.0.2

- Define elasticsearch settings

### 4.0.1

- Define gateway http max sizes
- Add support for DB less deployment

### 3.20.23

- "fix 'gravitee.yml' > 'services.metrics' definition from helm `values.yaml`"

### 3.20.20

- Allow wildcard in ingress host
- Remove smtp default example values
- Add unknownExpireAfter in management-api configuration

### 3.20.17

- Add revision history limit on portal
- Add podSecurityContext

- Avoid empty user when disabling admin user

### 3.20.16

- Define elasticsearch settings

### 3.20.15

- Define gateway http max sizes

### 3.19.20

- Define gateway http max sizes

### 4.0.0

- Remove old and unused `cache.type` from gateway config map
- Remove `api.removePlugins` & `gateway.removePlugins` as the platform allows plugin override now, only `additionalPlugins`  is necessary.

### 3.20.12

- Add support for managed Service Account for each product
- Fix resources missing in ui-deployment.yaml

### 3.19.17

- Add support for managed Service Account for each product
- Fix resources missing in ui-deployment.yaml

### 3.18.28

- Add support for managed Service Account for each product
- Fix resources missing in ui-deployment.yaml

### 3.20.11

- Change APIM charts versioning

To ensure the compatibility between the APIM product and the APIM Helm Chart, the versioning of the latter is changed.
As of 3.20.11, the APIM Helm Chart will follow APIM release cycle.
It means Helm Chart and APIM should always be aligned.

- Remove duplicate annotation in ui deployment

### 3.19.16

- Change APIM charts versioning

To ensure the compatibility between the APIM product and the APIM Helm Chart, the versioning of the latter is changed.
As of 3.19.16, the APIM Helm Chart will follow APIM release cycle.
It means Helm Chart and APIM should always be aligned.

- Remove duplicate annotation in ui deployment

### 3.18.27

- Change APIM charts versioning

To ensure the compatibility between the APIM product and the APIM Helm Chart, the versioning of the latter is changed.
As of 3.18.27, the APIM Helm Chart will follow APIM release cycle.
It means Helm Chart and APIM should always be aligned.

- Remove duplicate annotation in ui deployment

### 3.2.0

- Move Probes configuration under `deployment:`
- Change probe default configuration
- Remove `apiSync` parameter under `gateway.readinessProbe`
- Allow users to define their own `customStartupProbe` `customReadinessProbe` `customlivenessProbe`
- Allow disabling analytics in Management API
- Update default version of MongoDB in values. Set it to 6.0.6

- **BREAKING CHANGE**: Probes configuration has been changed. Check the default `values.yaml` before upgrade!

### 3.1.68

- Revert Previous commit "Remove old and unused `cache.type` from gateway configmap"

### 3.1.67

- Remove old and unused `cache.type` from gateway config map

### 3.1.66

- Add sni to the gateway configuration

### 3.1.65

- Add hook-delete-policy
- Allow users to use `logging.debug: true` when they define their own configuration file by defining a volume
- Add check to allow automatic Redis plugin download only for versions prior to 3.21.0 (it is now embedded in the distribution)
- Add SSL and Sentinel configuration management for Redis and updated doc. 
- Auto-generate `PORTAL_BASE_HREF` and `CONSOLE_BASE_HREF` environment variables
- Remove ingress nginx annotation when `ingress.class` is not nginx
- Add variables `api.ingress.management.scheme` and `api.ingress.portal.scheme` default `https`

### 3.1.64

- Add `gracefulShutdown` in gateway configuration

### 3.1.63

- Add support of user password policy config on API Management
- Add `externalTrafficPolicy` in service configuration
- Remove `email` block when smtp is disabled

### 3.1.62

- Remove `/gateway` from the gateway ingress path 
- Add `labels` in metrics config of the Management API and Gateway
- Fix default ingress path for ui
- Add API properties config of the Management API and Gateway
- Update APIM version to 3.20.1
- Update Elasticsearch version to 7.17.9
- Update MongoDB version to 5.0.14
- Add variable for ingress path type
- Remove legacy Kube Controller config. This feature is now provided by [Gravitee Operator for Kubernetes](https://github.com/gravitee-io/gravitee-kubernetes-operator)

### 3.1.61

- Fix AE alerts configuration without options
- Add values to define tracing options

### 3.1.60

- Add upgrader framework job
- Add additional Alert Engine configuration
- Add additional logback loggers configuration
- Clean Redis configuration in the `values.yaml` file
- Add documentation for alert engine connector ssl
- Add AE engine list support
- Allow users to define their own configuration file by defining a volume

### 3.1.59

- Replace ClusterRole with Role

### 3.1.58

- Add optional value to use gravitee licence key

### 3.1.57

- Provide advanced support to connect to the Hybrid Bridge

### 3.1.56

- Add support for Openshift Routes by removing ingress annotation
- Update Gravitee.io APIM v3.18.9

### 3.1.55

- Fix: Merge all smtp.properties directly into gravitee.yml
- Fix: Enable notifiers ssl with right smtp.ssl value

- **BREAKING CHANGE**: Use `smtp.properties.starttls.enable` instead of `smtp.properties.starttlsEnable`

### 3.1.54

- Add a startup probe on the Management API
- Truncate port name to respect k8s limit (15 for deployment and 63 for service)

### 3.1.53

- Remove alias for mongodb chart dependency

### 3.1.52

- Use ISO 8601 datetime for apim json logging

### 3.1.51

- Add JSON logging support

### 3.1.50

- Upgrade Mongodb and Elasticsearch dependencies
- Handle subscription service configuration

### 3.1.49

- Add Bridge service to Management API
- Add support for autoscaling/v2
- Add support for appProtocol to the services

- Update Gravitee.io APIM v3.15.10

### 3.1.48

- Add support for ingressClassName

### 3.1.47

- Fix Gateway Service Account for Kube Controller

- Update Gravitee.io APIM v3.15.9

### 3.1.46

- Add support for APIM console Pendo analytics with helm charts

### 3.1.45

- Add support for keystore and truststore in MongoDB configuration
- Add version labels on pods

### 3.1.44

- Add support for managed ServiceAccounts name provided by user

### 3.1.43

- Disable automatic download of Redis plugin

- Update Gravitee.io APIM v3.15.8

### 3.1.42

- Add support for PodDisruptionBudget

### 3.1.41

- Reorder HPA resources to avoid outofsync state with ArgoCD

### 3.1.40

- Fix ignoring the managedServiceAccount in the deployment files

### 3.1.39

- Set default array for topologySpreadConstraints

### 3.1.38

- Add support for topologySpreadConstraints

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

- Disable old classloader to enable the new one by default (since v3.15)
- Add support for kubernetes certificate configuration
- Make app.kubernetes.io/version label consistent
- Add quotes to version to fix #6450
- Update gravitee.io APIM v3.15.1

### 3.1.32

- Update gravitee.io APIM v3.11.3

### 3.1.31

- Add support for ILM managed indexes
- Remove the empty override of 'if-match' header

### 3.1.30 

- Add support for startupProbe

### 3.1.29

- Customization of the readinessProbe

### 3.1.28

- Support Ingress kubernetes >= 1.22.x
- Update gravitee.io APIM v3.11.2

### 3.1.27

- Manage redis repository plugin for nightly tag

### 3.1.25

- Extended configuration for init containers
- Support proxy at hybrid gateway level and system level

### 3.1.24

- Fix Management UI URL

### 3.1.23

- Fix init containers image repository and tag

### 3.1.22

- Configure init containers image repository and tag
- Update gravitee.io APIM v3.11.1

### 3.1.21

- Configure gateway sync service from ConfigMap

### 3.1.20

- Configure deployment strategy

### 3.1.19

- Provide a way to remove default plugins

### 3.1.18

- Fix alert-engine WS connection
- Configure ES timeout
- Fix typos in ES reporter lifecycle definitions
- Update gravitee.io APIM v3.10.0

### 3.1.17

- Fix issues with HTTP Bridge configuration

### 3.1.16

- Fix typo in API configmap

### 3.1.15

- Add support for tolerations for Portal

### 3.1.14

- Improve Deployment configurability

### 3.1.13

- Gateway Bridge Ingress based on networking.k8s.io

### 3.1.12

- Allow to configure Prometheus support for API Gateway and Management API
- Update gravitee.io APIM v3.8.4

### 3.1.11

- Allow to use the management node API as readinessProbe

### 3.1.10

- Fix Alert Engine configuration when alerting is disabled

### 3.1.9

- Rename Alert Engine connector when alerting is disabled

### 3.1.8

- Add support JDBC connection pool
- Fix baseURL generation issue
- Correctly evaluate ingress properties to customize constants.json file
- Enable only if the ssl dictionary is defined from values
- Downgrade required kube version to 1.14
- Update gravitee.io APIM v3.8.3

### 3.1.7

- Create an init container with JDBC driver for management API when JDBC is enabled

### 3.1.6

- Add support for policy configuration from gravitee.yml

### 3.1.5

- Client authentication support for HTTP bridge (hybrid deployment)

### 3.1.4

- [#94](https://github.com/gravitee-io/helm-charts/pull/94) Do not apply special treatment for nightly tag
- Update gravitee.io APIM v3.7.0

### 3.1.3

- Disable HTTP proxy by default

### 3.1.2

- Addition of configuration values for the kubernetes controller plugins
- Add configuration for Elasticsearch ILM
- Map handlers for API Gateway to override X-Transaction and X-Request
- Portal extraInitContainers mapped to ui

### 3.1.1

- Add support for extended reporters configurations
- Allow to configure trustAll for identity providers

### 3.1.0

- Manage default plugins
- Update gravitee.io APIM v3.6.0

### 3.0.21

- Add authSource field to ratelimit section
- Update gravitee.io APIM v3.5.3

### 3.0.20

- Allows to deploy nightly version of APIM
- Add mongo authSource field to configmaps when not using Mongo URI

### 3.0.19

- Externalize securityContext from deployments

### 3.0.18

- Enable technical api for api and gateway by default
- Add sane default value for technical api password
- Add gateway technical api spec to values.yaml
- Add technical ingress to gateway, like what was done for api-component
- Add technical api spec to gateway service
- Parametrize technical api portion of gateway configmap
- Add technical api enable flag and auth type options to api-configmap
- Update ui-deployment.yaml
- Minor ingress improvements 

### 3.0.17

- Update gravitee.io APIM v3.5.2

### 3.0.16

- Update gravitee.io APIM v3.5.0

### 3.0.15

- Fix mongo env var overloading using helm
- Add default company name to values.yaml
- Add company name to template
- Update Helm dependencies repository
- Allow to configure the fullname of services, deployments, ...
- Add condition for replica setting
- Simplify configuration of jdbc driver and other plugins

### 3.0.14

- Add custom management console URL handling resolve

- Update gravitee.io APIM v3.4.0

### 3.0.13

- Allow the configuration of an OIDC provider
- Fix helm requirements
- Add support for sidecar containers
- Add APIM redis example + remove values examples

### 3.0.12

- Add support for HTTP bridge deployment
- Fix issue with smtp templating values

- Update gravitee.io APIM v3.3.3
