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
      - gravitee_integration_providers_0_configuration_url=\${SOLACE_ENDPOINT:-https://apim-production-api.solace.cloud/api/v2/apim}
      # optionals
      - gravitee_integration_providers_0_configuration_0_appDomains=\${SOLACE_APPLICATION_0_DOMAIN}
      - gravitee_integration_providers_0_configuration_1_appDomains=\${SOLACE_APPLICATION_1_DOMAIN}
`;

export const configurationCode = {
  'aws-api-gateway': awsConfigurationCode,
  solace: solaceConfigurationCode,
};
