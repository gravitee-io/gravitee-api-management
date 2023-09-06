import { DockerResourceClass } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Executors/types/DockerExecutor.types';

const cache = {
  prefix: 'gravitee-api-management-v9',
};

const dockerImages = {
  cacheDir: '/tmp/docker-cache',
  gateway: 'apim-gateway',
  managementApi: 'apim-management-api',
  console: 'apim-management-ui',
  portal: 'apim-portal-ui',
};

const executor = {
  machine: {
    forTests: 'ubuntu-2204:2022.04.2',
  },
  base: 'cimg/base:stable',
  node: 'cimg/node:16.10',
  openjdk: {
    image: 'cimg/openjdk',
    resource: 'medium' as DockerResourceClass,
    version: '17.0',
  },
};

const maven = {
  settingsFile: '.gravitee.settings.xml',
};

const orbs = {
  artifactory: '1.0.1',
  awsCli: '2.0.6',
  awsS3: '3.1.1',
  github: '1.0.5',
  helm: '2.0.1',
  keeper: '0.6.3',
  slack: '4.12.5',
  snyk: '1.7.0',
};

const secrets = {
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

const ssh = {
  fingerprints: ['ac:88:23:8f:c6:0f:7d:f0:fc:df:73:20:34:56:02:6c'],
};

const jobContext = ['cicd-orchestrator'];

const slack = {
  channels: {
    apiManagementTeamNotifications: 'C02JENTV2AX',
    graviteeReleaseAlerts: 'C02NGT20S4W',
  },
};

export const config = {
  cache,
  dockerImages,
  executor,
  jobContext,
  maven,
  orbs,
  secrets,
  slack,
  ssh,
};
