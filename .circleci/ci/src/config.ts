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
const artifactoryUrl = 'https://odbxikk7vo-artifactory.services.clever-cloud.com';

const cache = {
  prefix: 'gravitee-api-management-v10',
};

const dockerImages = {
  cacheDir: '/tmp/docker-cache',
  gateway: {
    project: 'gravitee-apim-gateway',
    image: 'apim-gateway',
  },
  managementApi: {
    project: 'gravitee-apim-rest-api',
    image: 'apim-management-api',
  },
  console: {
    project: 'gravitee-apim-console-webui',
    image: 'apim-management-ui',
  },
  portal: {
    project: 'gravitee-apim-portal-webui',
    image: 'apim-portal-ui',
  },
};

const executor = {
  azure: {
    // Version can be found here https://docs.microsoft.com/en-us/cli/azure/release-notes-azure-cli
    // be careful when updating the version as it looks it is not following semver
    image: 'mcr.microsoft.com/azure-cli',
    version: '2.34.1',
  },
  base: {
    image: 'cimg/base',
    version: 'stable',
  },
  openjdk: {
    image: 'cimg/openjdk',
    version: '17.0.8', // starting with 17.0.9, node version becomes 20.9.0, which breaks the build.
  },
  node: {
    image: 'cimg/node',
    version: '16.10',
  },
  sonar: {
    image: 'sonarsource/sonar-scanner-cli',
    version: '5.0.1',
  },
  ubuntu: {
    version: '2204',
    tag: 'current',
  },
};

const helm = {
  defaultVersion: 'v3.12.3',
  helmUnitVersion: '0.5.1',
};

const jobContext = ['cicd-orchestrator'];

const maven = {
  settingsFile: '.gravitee.settings.xml',
};

const orbs = {
  aquasec: '1.0.0',
  artifactory: '1.0.1',
  awsCli: '2.0.6',
  awsS3: '3.0.0',
  github: '1.0.5',
  helm: '3.0.0',
  keeper: '0.6.3',
  slack: '4.12.5',
  snyk: '1.7.0',
};

const secrets = {
  aquaKey: 'keeper://QeHHkvALPob4pgs1hMd9Gw/custom_field/API Key',
  aquaSecret: 'keeper://QeHHkvALPob4pgs1hMd9Gw/custom_field/Secret',
  aquaScannerKey: 'keeper://QeHHkvALPob4pgs1hMd9Gw/custom_field/ScannerToken',
  artifactoryApiKey: 'keeper://R7NuqoW0KD-8l-kjx0-PgQ/field/password',
  artifactoryUser: 'keeper://R7NuqoW0KD-8l-kjx0-PgQ/field/login',
  awsAccessKeyId: 'keeper://Mqmplmfu17bDR5XRLmO1mQ/field/password',
  awsSecretAccessKey: 'keeper://3-pU56sIqcyWWw7HxhxjaQ/field/password',
  azureApplicationId: 'keeper://UryantA7MvZe8fkWwcUt8g/field/login',
  azureApplicationSecret: 'keeper://UryantA7MvZe8fkWwcUt8g/field/password',
  azureRegistryPassword: 'keeper://Q721P2LSOPJ9qiXLuf5AHQ/field/password',
  azureRegistryUsername: 'keeper://Q721P2LSOPJ9qiXLuf5AHQ/field/login',
  azureTenant: 'keeper://UryantA7MvZe8fkWwcUt8g/custom_field/tenant',
  chromaticProjectToken: 'keeper://Hp1bFl5s0doxnQgqkMdCdg/field/password',
  circleCiToken: 'keeper://G4hBnFUDBYb9Sw3TxhvjHg/custom_field/token',
  dockerhubBotUserName: 'keeper://cooU9UoXIk8Kj0hsP2rkBw/field/login',
  dockerhubBotUserToken: 'keeper://cooU9UoXIk8Kj0hsP2rkBw/field/password',
  githubApiToken: 'keeper://TIlcGPFq4rN5GvgnZb9hng/field/password',
  gitUserEmail: 'keeper://IZd-yvsMopfQEa_0j1SDvg/custom_field/email',
  gitUserName: 'keeper://IZd-yvsMopfQEa_0j1SDvg/field/login',
  gpgPublicKey: 'keeper://riW92t8X4tk4ZmQc8-FZ4Q/custom_field/armor_format_pub_key',
  gpgPrivateKey: 'keeper://riW92t8X4tk4ZmQc8-FZ4Q/custom_field/armor_format_private_key',
  graviteeLicense: 'keeper://w8WBpALVCgYdxtV5pVrQsw/custom_field/base64',
  graviteePackageCloudToken: 'keeper://8CG6HxY5gYsl-85eJKuIoA/field/password',
  jiraToken: 'keeper://hfnQD5TEfxzwRXUKhJhM-A/field/password',
  mavenSettings: 'keeper://7CgijuGiFDSLynRJt1Dm9w/custom_field/xml',
  slackAccessToken: 'keeper://ZOz4db245GNaETVwmPBk8w/field/password',
  snykApiToken: 'keeper://s83JmReKpBZWjHdud6ZAlg/custom_field/gravitee_apim_org_api_token',
  snykDockerHubIntegrationId: 'keeper://s83JmReKpBZWjHdud6ZAlg/custom_field/gravitee_apim_dockerhub_integration_id',
  snykIntegrationId: 'keeper://s83JmReKpBZWjHdud6ZAlg/custom_field/gravitee_apim_acr_integration_id',
  snykOrgId: 'keeper://s83JmReKpBZWjHdud6ZAlg/custom_field/gravitee_apim_org_id',
  sonarToken: 'keeper://9x9YgyU6DWzux4DPoHAzDQ/field/password',
};

const slack = {
  channels: {
    apiManagementTeamNotifications: 'C02JENTV2AX',
    graviteeReleaseAlerts: 'C02NGT20S4W',
  },
};

const ssh = {
  fingerprints: ['ac:88:23:8f:c6:0f:7d:f0:fc:df:73:20:34:56:02:6c'],
};

const docker = {
  version: 'default',
};

export const config = {
  artifactoryUrl,
  cache,
  dockerImages,
  executor,
  helm,
  jobContext,
  maven,
  orbs,
  secrets,
  slack,
  ssh,
  docker,
};
