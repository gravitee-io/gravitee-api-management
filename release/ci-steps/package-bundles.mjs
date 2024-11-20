#!/usr/bin/env zx

import xml2json from 'xml2json';

console.log(chalk.magenta(`######################################`));
console.log(chalk.magenta(`# 📦 Prepare APIM package bundles 📦 #`));
console.log(chalk.magenta(`######################################`));

console.log(chalk.blue(`Step 1: Prepare APIM distribution`));
await within(async () => {
  cd('../gravitee-apim-distribution');
  // https://maven.apache.org/plugins/maven-help-plugin/effective-pom-mojo.html
  // `help:effective-pom` allows to get the pom.xml with all the inherited properties
  // `-Dbundle` activate the "bundle" profile (from distribution/pom.xml). This maven profile adds some EE plugins in the release.
  await $`mvn help:effective-pom -Doutput=.effective-pom.xml -Dbundle`;
});

const pomXml = await fs.readFile(`../gravitee-apim-distribution/.effective-pom.xml`, 'utf-8');
const jsonPom = JSON.parse(await xml2json.toJson(pomXml, {}));

const releasingVersion = argv.version;
const tmpPath = `./.tmp/${releasingVersion}`;

const distributionDependencies = jsonPom.project.dependencies.dependency
  .filter((dependency) => dependency.scope === 'runtime' && dependency.type === 'zip')
  .map((dependency) => ({
    ...dependency,
  }));

console.log(chalk.blue(`Step 2: Download all dependencies from artifactory`));
await $`mkdir -p ${tmpPath}`;
cd(tmpPath);

console.log(chalk.yellow(`Dependencies:`));

const allDependencies = [
  ...distributionDependencies,
  {
    groupId: 'io.gravitee.apim.ui',
    artifactId: 'gravitee-apim-console-webui',
    version: releasingVersion,
    type: 'zip',
  },
  {
    groupId: 'io.gravitee.apim.ui',
    artifactId: 'gravitee-apim-portal-webui',
    version: releasingVersion,
    type: 'zip',
  },
  {
    groupId: 'io.gravitee.apim.rest.api.standalone.distribution',
    artifactId: 'gravitee-apim-rest-api-standalone-distribution-zip',
    version: releasingVersion,
    type: 'zip',
  },
  {
    groupId: 'io.gravitee.apim.gateway.standalone.distribution',
    artifactId: 'gravitee-apim-gateway-standalone-distribution-zip',
    version: releasingVersion,
    type: 'zip',
  },
].map((dependency) => {
  const fileName = `${dependency.artifactId}-${dependency.version}.${dependency.type}`;
  console.log(chalk.yellow(` - ${dependency.artifactId}: ${dependency.version}`));
  const path = `${dependency.groupId.replaceAll('.', '/')}/${dependency.artifactId}/${dependency.version}`;
  return {
    ...dependency,
    fileName,
    downloadUrl: `${process.env.ARTIFACTORY_REPO_URL}/${path}/${fileName}`,
  };
});
const allDependenciesMap = new Map(
  allDependencies.map((d) => {
    return [d.artifactId, d];
  }),
);

const createFileSum = async (file) => {
  await $`md5sum ${file} > ${file}.md5`;
  await $`sha512sum ${file} > ${file}.sha512sum`;
  await $`sha1sum ${file} > ${file}.sha1`;
};

console.log(chalk.blue(`Downloading all dependencies from artifactory:`));
await Promise.all(
  allDependencies.map(async ({ downloadUrl }) => {
    try {
      await $`curl -u ${process.env.ARTIFACTORY_USERNAME}:${process.env.ARTIFACTORY_PASSWORD} -f -O ${downloadUrl}`;
      console.log(chalk.yellow(` -  ✅ ${downloadUrl}`));
      return Promise.resolve(downloadUrl);
    } catch (e) {
      console.log(chalk.red(`Failed to download ${downloadUrl}`));
      return Promise.reject();
    }
  }),
);

console.log(chalk.green(`\n   ${allDependencies.length} dependencies downloaded`));

console.log(chalk.blue(`Step 3; Packaging - Components / Rest API`));
const restApiComponentDir = `./dist/components/gravitee-management-rest-api`;
await $`rm -rf ${restApiComponentDir} && mkdir -p ${restApiComponentDir}`;

await $`cp ${allDependenciesMap.get('gravitee-apim-rest-api-standalone-distribution-zip').fileName} ${restApiComponentDir}`;

await within(async () => {
  cd(`${restApiComponentDir}`);
  await $`mv ${
    allDependenciesMap.get('gravitee-apim-rest-api-standalone-distribution-zip').fileName
  } gravitee-apim-rest-api-${releasingVersion}.zip`;
  await createFileSum(`gravitee-apim-rest-api-${releasingVersion}.zip`);
});

console.log(chalk.blue(`Step 4: Packaging - Components / Gateway`));
const gatewayComponentDir = `./dist/components/gravitee-gateway`;
await $`rm -rf ${gatewayComponentDir} && mkdir -p ${gatewayComponentDir}`;

await $`cp ${allDependenciesMap.get('gravitee-apim-gateway-standalone-distribution-zip').fileName} ${gatewayComponentDir}`;

await within(async () => {
  cd(`${gatewayComponentDir}`);
  await $`mv ${
    allDependenciesMap.get('gravitee-apim-gateway-standalone-distribution-zip').fileName
  } gravitee-apim-gateway-${releasingVersion}.zip`;
  await createFileSum(`gravitee-apim-gateway-${releasingVersion}.zip`);
});

console.log(chalk.blue(`Step 5: Packaging - Components / Console`));
const consoleComponentDir = `./dist/components/gravitee-management-webui`;
await $`rm -rf ${consoleComponentDir} && mkdir -p ${consoleComponentDir}`;

await $`cp ${allDependenciesMap.get('gravitee-apim-console-webui').fileName} ${consoleComponentDir}/`;
await createFileSum(`${consoleComponentDir}/${allDependenciesMap.get('gravitee-apim-console-webui').fileName}`);

console.log(chalk.blue(`Step 6: Packaging - Components / Portal`));
const portalComponentDir = `./dist/components/gravitee-portal-webui`;
await $`rm -rf ${portalComponentDir} && mkdir -p ${portalComponentDir}`;

await $`cp ${allDependenciesMap.get('gravitee-apim-portal-webui').fileName} ${portalComponentDir}/`;
await createFileSum(`${portalComponentDir}/${allDependenciesMap.get('gravitee-apim-portal-webui').fileName}`);

console.log(chalk.blue(`Step 7: Packaging - Distribution / Full`));
// TODO: Remove duplicated graviteeio-full-${releasingVersion} directory
const fullDistributionDir = `./dist/distributions/graviteeio-full-${releasingVersion}/graviteeio-full-${releasingVersion}`;
await $`rm -rf ${fullDistributionDir} && mkdir -p ${fullDistributionDir}`;

// Console
await $`unzip -q -o ${allDependenciesMap.get('gravitee-apim-console-webui').fileName} -d ${fullDistributionDir}`;

// Portal
await $`unzip -q -o ${allDependenciesMap.get('gravitee-apim-portal-webui').fileName} -d ${fullDistributionDir}`;

// Rest API
await $`unzip -q -o ${restApiComponentDir}/gravitee-apim-rest-api-${releasingVersion}.zip -d ${fullDistributionDir}`;
await $`mv ${fullDistributionDir}/gravitee-apim-rest-api-standalone-${releasingVersion} ${fullDistributionDir}/gravitee-apim-rest-api-${releasingVersion}`;
const restApiDependenciesExclusion = [
  // GroupIds to exclude
  'io.gravitee.apim.ui',
  'io.gravitee.apim.gateway.standalone.distribution',
  'io.gravitee.apim.rest.api.standalone.distribution',
  'com.graviteesource.reactor',
  'com.graviteesource.reporter',
  'io.gravitee.reporter',
  'io.gravitee.tracer',
  // ArtifactIds to exclude
  'gravitee-apim-repository-bridge-http-client',
  'gravitee-apim-repository-redis',
  'gravitee-gateway-services-ratelimit',
];

console.log(chalk.blue(`Add plugins to Rest API`));
await Promise.all(
  allDependencies
    .filter((d) => !restApiDependenciesExclusion.includes(d.artifactId) && !restApiDependenciesExclusion.includes(d.groupId))
    .map(({ fileName }) => $`cp ${fileName} ${fullDistributionDir}/gravitee-apim-rest-api-${releasingVersion}/plugins`),
);

// Gateway
await $`unzip -q -o ${gatewayComponentDir}/gravitee-apim-gateway-${releasingVersion}.zip -d ${fullDistributionDir}`;
await $`mv ${fullDistributionDir}/gravitee-apim-gateway-standalone-${releasingVersion} ${fullDistributionDir}/gravitee-apim-gateway-${releasingVersion}`;
const gatewayDependenciesExclusion = [
  // GroupIds to exclude
  'io.gravitee.apim.ui',
  'io.gravitee.apim.gateway.standalone.distribution',
  'io.gravitee.apim.rest.api.standalone.distribution',
  'io.gravitee.fetcher',
  'io.gravitee.notifier',
  // ArtifactIds to exclude
  'gravitee-apim-repository-elasticsearch',
  'gravitee-cockpit-connectors-ws',
  'gravitee-apim-plugin-apiservice-dynamicproperties-http',
  'gravitee-endpoint-native-kafka',
  'gravitee-entrypoint-native-kafka',
];

console.log(chalk.blue(`Add plugins to Gateway`));
await Promise.all(
  allDependencies
    .filter((d) => !gatewayDependenciesExclusion.includes(d.artifactId) && !gatewayDependenciesExclusion.includes(d.groupId))
    .map(async ({ fileName }) => $`cp ${fileName} ${fullDistributionDir}/gravitee-apim-gateway-${releasingVersion}/plugins`),
);

// TODO: Remove the following lines to align names of these components to match the naming convention `gravitee-*` and not `graviteeio-*`
await $`mv ${fullDistributionDir}/gravitee-apim-console-webui-${releasingVersion} ${fullDistributionDir}/graviteeio-apim-console-ui-${releasingVersion}`;
await $`mv ${fullDistributionDir}/gravitee-apim-portal-webui-${releasingVersion} ${fullDistributionDir}/graviteeio-apim-portal-ui-${releasingVersion}`;
await $`mv ${fullDistributionDir}/gravitee-apim-gateway-${releasingVersion} ${fullDistributionDir}/graviteeio-apim-gateway-${releasingVersion}`;
await $`mv ${fullDistributionDir}/gravitee-apim-rest-api-${releasingVersion} ${fullDistributionDir}/graviteeio-apim-rest-api-${releasingVersion}`;

await within(async () => {
  cd(`${fullDistributionDir}`);
  cd(`../`);
  await $`zip -q -r ../graviteeio-full-${releasingVersion}.zip .`;
  cd(`../`);
  await createFileSum(`graviteeio-full-${releasingVersion}.zip`);
  await $`rm -rf graviteeio-full-${releasingVersion}`;
});

console.log(chalk.blue(`Step 8: Packaging - Plugins / Repositories`));
const packagePlugins = async (pluginsDir, pluginsGroupId) => {
  await $`rm -rf ${pluginsDir} && mkdir -p ${pluginsDir}`;

  await Promise.all(
    allDependencies
      .filter((d) => d.groupId.startsWith(pluginsGroupId))
      .map(async ({ fileName, artifactId }) => {
        await $`mkdir -p  ${pluginsDir}/${artifactId} && cp ${fileName} ${pluginsDir}/${artifactId}/`;
        await createFileSum(`${pluginsDir}/${artifactId}/${fileName}`);
      }),
  );
};

// Repositories
await packagePlugins('./dist/plugins/repositories', 'io.gravitee.apim.repository');
await packagePlugins('./dist/plugins/endpoints', 'io.gravitee.apim.plugin.endpoint');
await packagePlugins('./dist/plugins/entrypoints', 'io.gravitee.apim.plugin.entrypoint');

console.log(chalk.magenta(`📦 The package is ready!`));
