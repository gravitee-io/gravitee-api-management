/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const apigeeConfigurationCode = `
version: '3.8'

services:
  integration-agent:
    image: \${APIM_REGISTRY:-graviteeio}/federation-agent-apigee:\${AGENT_VERSION:-latest}
    restart: always
    volumes:
      - \${SERVICE_ACCOUNT_KEY_PATH:-/dev/null}:/opt/graviteeio-integration-agent/config/key/key.json
    environment:
      - gravitee_integration_connector_ws_endpoints_0=\${WS_ENDPOINTS}
      - gravitee_integration_connector_ws_headers_0_name=Authorization
      - gravitee_integration_connector_ws_headers_0_value=bearer \${WS_AUTH_TOKEN}
      - gravitee_integration_providers_0_integrationId=\${INTEGRATION_ID}
      - gravitee_integration_providers_0_configuration_gcpProjectId=\${GCP_PROJECT_ID}
      - gravitee_integration_providers_0_configuration_developerEmail=\${APIGEE_DEV_EMAIL}
      - gravitee_integration_providers_0_configuration_developerFirstName=\${APIGEE_DEV_FIRST_NAME}
      - gravitee_integration_providers_0_configuration_developerLastName=\${APIGEE_DEV_LAST_NAME}
      - gravitee_integration_providers_0_configuration_developerUsername=\${APIGEE_DEV_USERNAME}
      - gravitee_integration_providers_0_configuration_serviceAccountKeyInline=\${SERVICE_ACCOUNT_KEY_INLINE}
      - gravitee_integration_providers_0_type=apigee
      - GOOGLE_APPLICATION_CREDENTIALS=/opt/graviteeio-integration-agent/config/key/key.json
`;

const awsConfigurationCode = `
version: '3.8'

services:
  integration-agent:
    image: \${APIM_REGISTRY:-graviteeio}/federation-agent-aws-api-gateway:\${AGENT_VERSION:-latest}
    restart: always
    environment:
      - gravitee_integration_connector_ws_endpoints_0=\${WS_ENDPOINTS}
      - gravitee_integration_connector_ws_headers_0_name=Authorization
      - gravitee_integration_connector_ws_headers_0_value=bearer \${WS_AUTH_TOKEN}
      - gravitee_integration_providers_0_configuration_accessKeyId=\${AWS_ACCESS_KEY_ID}
      - gravitee_integration_providers_0_configuration_region=\${AWS_REGION}
      - gravitee_integration_providers_0_configuration_secretAccessKey=\${AWS_SECRET_ACCESS_KEY}
      - gravitee_integration_providers_0_integrationId=\${INTEGRATION_ID}
      - gravitee_integration_providers_0_type=aws-api-gateway
`;

const solaceConfigurationCode = `
version: '3.8'

services:
  integration-agent:
    image: \${APIM_REGISTRY:-graviteeio}/federation-agent-solace:\${AGENT_VERSION:-latest}
    restart: always
    environment:
      - gravitee_integration_connector_ws_endpoints_0=\${WS_ENDPOINTS}
      - gravitee_integration_connector_ws_headers_0_name=Authorization
      - gravitee_integration_connector_ws_headers_0_value=bearer \${WS_AUTH_TOKEN}
      - gravitee_integration_providers_0_configuration_authToken=\${SOLACE_AUTH_TOKEN}
      - gravitee_integration_providers_0_integrationId=\${INTEGRATION_ID}
      - gravitee_integration_providers_0_type=solace
      - gravitee_integration_providers_0_configuration_url=\${SOLACE_ENDPOINT:-https://api.solace.cloud/api/v2/architecture}
- gravitee_integration_providers_0_configuration_0_appDomains=\${SOLACE_APPLICATION_0_DOMAIN:-}
- gravitee_integration_providers_0_configuration_1_appDomains=\${SOLACE_APPLICATION_1_DOMAIN:-}
`;

const azureConfigurationCode = `
version: '3.8'

services:
  integration-agent:
    image: \${APIM_REGISTRY:-graviteeio}/federation-agent-azure-api-management:\${AGENT_VERSION:-latest}
    restart: always
    environment:
      - gravitee_integration_connector_ws_endpoints_0=\${WS_ENDPOINTS}
      - gravitee_integration_connector_ws_headers_0_name=Authorization
      - gravitee_integration_connector_ws_headers_0_value=Bearer \${WS_AUTH_TOKEN}
      - gravitee_integration_providers_0_integrationId=\${INTEGRATION_ID}
      - gravitee_integration_providers_0_type=azure-api-management
      # authentication
      - gravitee_integration_providers_0_configuration_auth_appId=\${APP_ID}
      - gravitee_integration_providers_0_configuration_auth_appSecret=\${APP_SECRET}
      - gravitee_integration_providers_0_configuration_auth_tenant=\${TENANT_ID}
      # others configs
      - gravitee_integration_providers_0_configuration_subscription=\${SUBSCRIPTION}
      - gravitee_integration_providers_0_configuration_resourceGroup=\${RESOURCE_GROUP}
      - gravitee_integration_providers_0_configuration_service=\${SERVICE}
      # subscription
      - gravitee_integration_providers_0_configuration_dev_email=\${AZURE_DEV_EMAIL}
      - gravitee_integration_providers_0_configuration_dev_firstName=\${AZURE_DEV_FIRST_NAME}
      - gravitee_integration_providers_0_configuration_dev_lastName=\${AZURE_DEV_LAST_NAME}
`;

const ibmConfigurationCode = `
version: '3.8'

services:
  integration-agent:
    image: \${APIM_REGISTRY:-graviteeio}/federation-agent-ibm-api-connect:\${AGENT_VERSION:-latest}
    restart: always
    environment:
      - gravitee_integration_connector_ws_endpoints_0=\${WS_ENDPOINTS}
      - gravitee_integration_connector_ws_headers_0_name=Authorization
      - gravitee_integration_connector_ws_headers_0_value=bearer \${WS_AUTH_TOKEN}
      - gravitee_integration_providers_0_integrationId=\${INTEGRATION_ID}
      - gravitee_integration_providers_0_configuration_platformApiUrl=\${PLATFORM_API_URL}
      - gravitee_integration_providers_0_configuration_clientId=\${CLIENT_ID}
      - gravitee_integration_providers_0_configuration_clientSecret=\${CLIENT_SECRET}
      - gravitee_integration_providers_0_configuration_organizationName=\${ORGANIZATION_NAME}
      - gravitee_integration_providers_0_configuration_apiKey=\${API_KEY}
      - gravitee_integration_providers_0_type=ibm-api-connect
`;

const confluentPlatformConfigurationCode = `
version: "3.8"

services:
    integration-agent:
        image: \${APIM_REGISTRY:-graviteeio}/federation-agent-confluent-platform:\${AGENT_VERSION:-latest}
        restart: always
        environment:
            - gravitee_integration_connector_ws_endpoints_0=\${WS_ENDPOINTS}
            - gravitee_integration_connector_ws_headers_0_name=Authorization
            - gravitee_integration_connector_ws_headers_0_value=Bearer \${WS_AUTH_TOKEN}
            - gravitee_integration_providers_0_integrationId=\${INTEGRATION_ID}
            - gravitee_integration_providers_0_type=confluent-platform
            - gravitee_integration_providers_0_configuration_cluster_api_endpoint=\${CLUSTER_API_ENDPOINT}
            - gravitee_integration_providers_0_configuration_schema_registry_endpoint=\${SCHEMA_REGISTRY_ENDPOINT}
            - gravitee_integration_providers_0_configuration_auth_password=\${BASIC_AUTH_LOGIN:-}
            - gravitee_integration_providers_0_configuration_auth_username=\${BASIC_AUTH_PASSWORD:-}
            - gravitee_integration_providers_0_configuration_topic_prefix=\${PREFIX:-}
            - gravitee_integration_providers_0_configuration_trust_all=\${TRUST_ALL:-}
`;

export const configurationCode = {
  'aws-api-gateway': awsConfigurationCode,
  solace: solaceConfigurationCode,
  apigee: apigeeConfigurationCode,
  'azure-api-management': azureConfigurationCode,
  'ibm-api-connect': ibmConfigurationCode,
  'confluent-platform': confluentPlatformConfigurationCode,
};
