/**
 * Gravitee APIM CI configuration constants.
 * Mirror of .circleci/ci/src/config.ts
 */

export const aqua = {
  scannerUrl: 'https://82fb8f75da.cloud.aquasec.com',
};

export const artifactoryUrl = 'https://odbxikk7vo-artifactory.services.clever-cloud.com';

export const awsCliVersion = '2.22.35';
export const awsECRUrl = '430630701098.dkr.ecr.eu-west-2.amazonaws.com';

export const cachePrefix = 'gravitee-api-management-v14';

export const components = {
  cacheDir: '/tmp/docker-cache',
  gateway: { project: 'gravitee-apim-gateway', image: 'apim-gateway' },
  managementApi: { project: 'gravitee-apim-rest-api', image: 'apim-management-api' },
  console: { project: 'gravitee-apim-console-webui', image: 'apim-management-ui' },
  portal: {
    project: 'gravitee-apim-portal-webui',
    image: 'apim-portal-ui',
    next: { project: 'gravitee-apim-portal-webui-next' },
  },
};

export const executorImages = {
  azure: { image: 'mcr.microsoft.com/azure-cli', version: '2.34.1' },
  base: { image: 'cimg/base', version: 'stable' },
  openjdk: { image: 'cimg/openjdk', version: '21.0.5' },
  node: { image: 'cimg/node', version: '22.12.0' },
  sonar: { image: 'sonarsource/sonar-scanner-cli', version: '11.2' },
  ubuntu: { version: '2204', tag: 'current' },
};

export const helmVersions = {
  defaultVersion: 'v3.12.3',
  helmUnitVersion: '0.5.1',
};

export const jobContext = ['cicd-orchestrator'];

export const maven = {
  settingsFile: '.gravitee.settings.xml',
};

export const yarnVersion = '4.1.1';

export const orbVersions = {
  aquasec: '1.0.5',
  artifactory: '1.0.1',
  awsCli: '5.1.2',
  awsS3: '4.1.0',
  github: '1.0.5',
  gravitee: 'dev:4.5.0',
  helm: '3.0.0',
  keeper: '0.7.0',
  slack: '4.12.5',
};

export const secrets = {
  aquaKey: 'keeper://QeHHkvALPob4pgs1hMd9Gw/custom_field/API Key',
  aquaSecret: 'keeper://QeHHkvALPob4pgs1hMd9Gw/custom_field/Secret',
  aquaScannerKey: 'keeper://QeHHkvALPob4pgs1hMd9Gw/custom_field/ScannerToken',
  aquaRegistryUsername: 'keeper://LYg-wdlM5UDzSqFFH6Kyig/field/login',
  aquaRegistryPassword: 'keeper://LYg-wdlM5UDzSqFFH6Kyig/field/password',
  artifactoryApiKey: 'keeper://R7NuqoW0KD-8l-kjx0-PgQ/field/password',
  artifactoryUser: 'keeper://R7NuqoW0KD-8l-kjx0-PgQ/field/login',
  awsAccessKeyId: 'keeper://Mqmplmfu17bDR5XRLmO1mQ/field/password',
  awsSecretAccessKey: 'keeper://3-pU56sIqcyWWw7HxhxjaQ/field/password',
  awsHelmAccessKeyId: 'keeper://AEmKkeWZ4Zq758pvaPIE1A/custom_field/aws_access_key_id',
  awsHelmSecretAccessKey: 'keeper://AEmKkeWZ4Zq758pvaPIE1A/custom_field/aws_secret_access_key',
  awsHelmRegion: 'keeper://AEmKkeWZ4Zq758pvaPIE1A/custom_field/aws_region',
  azureApplicationId: 'keeper://UryantA7MvZe8fkWwcUt8g/field/login',
  azureApplicationSecret: 'keeper://UryantA7MvZe8fkWwcUt8g/field/password',
  azureRegistryPassword: 'keeper://Q721P2LSOPJ9qiXLuf5AHQ/field/password',
  azureRegistryUsername: 'keeper://Q721P2LSOPJ9qiXLuf5AHQ/field/login',
  azureTenant: 'keeper://UryantA7MvZe8fkWwcUt8g/custom_field/tenant',
  chromaticProjectToken: 'keeper://Hp1bFl5s0doxnQgqkMdCdg/field/password',
  cypressCloudKey: 'keeper://FVzylPCgY-LJsgjkW4q1eQ/field/password',
  circleCiToken: 'keeper://G4hBnFUDBYb9Sw3TxhvjHg/custom_field/token',
  dockerhubBotUserName: 'keeper://cooU9UoXIk8Kj0hsP2rkBw/field/login',
  dockerhubBotUserToken: 'keeper://cooU9UoXIk8Kj0hsP2rkBw/field/password',
  githubApiToken: 'keeper://TIlcGPFq4rN5GvgnZb9hng/field/password',
  gitUserEmail: 'keeper://IZd-yvsMopfQEa_0j1SDvg/custom_field/email',
  gitUserName: 'keeper://IZd-yvsMopfQEa_0j1SDvg/field/login',
  gpgPublicKey: 'keeper://riW92t8X4tk4ZmQc8-FZ4Q/custom_field/armor_format_pub_key',
  gpgPrivateKey: 'keeper://riW92t8X4tk4ZmQc8-FZ4Q/custom_field/armor_format_private_key',
  gpgKeyName: 'keeper://riW92t8X4tk4ZmQc8-FZ4Q/field/login',
  gpgKeyPassphrase: 'keeper://riW92t8X4tk4ZmQc8-FZ4Q/custom_field/passphrase',
  graviteeLicense: 'keeper://w8WBpALVCgYdxtV5pVrQsw/custom_field/base64',
  graviteePackageCloudToken: 'keeper://8CG6HxY5gYsl-85eJKuIoA/field/password',
  jiraToken: 'keeper://hfnQD5TEfxzwRXUKhJhM-A/field/password',
  mavenSettings: 'keeper://7CgijuGiFDSLynRJt1Dm9w/custom_field/xml',
  slackAccessToken: 'keeper://ZOz4db245GNaETVwmPBk8w/field/password',
  sonarToken: 'keeper://9x9YgyU6DWzux4DPoHAzDQ/field/password',
};

export const slackChannels = {
  apiManagementTeamNotifications: 'C02JENTV2AX',
  apimCiNotificationsBridgeCompatibilityTests: 'C08H9K43WSV',
  apimCiNotificationsHelmTests: 'C08GWL40VB7',
  apimCiNotificationsRepositoriesTests: 'C08HMNYPHA4',
  graviteeReleaseAlerts: 'C02NGT20S4W',
};

export const sshFingerprints = ['ac:88:23:8f:c6:0f:7d:f0:fc:df:73:20:34:56:02:6c'];

export const dockerVersion = 'default';
