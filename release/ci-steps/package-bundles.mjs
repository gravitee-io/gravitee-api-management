#!/usr/bin/env zx

import xml2json from 'xml2json';

console.log(chalk.magenta(`######################################`));
console.log(chalk.magenta(`# 📦 Prepare APIM package bundles 📦 #`));
console.log(chalk.magenta(`######################################`));

console.log(chalk.blue(`Step 1: Prepare APIM distribution`));
await within(async () => {
  cd('../');
  await $`mvn flatten:flatten`;
});

const pomXml = await fs.readFile(`../gravitee-apim-distribution/.flattened-pom.xml`, 'utf-8');
const jsonPom = JSON.parse(await xml2json.toJson(pomXml, {}));

const releasingVersion = argv.version; // jsonPom.project.parent.version;
const tmpPath = `./.tmp/${releasingVersion}`;

const resolveLinkedVersion = (allVersions, valueToResolve) => {
  const versionLinkMatch = valueToResolve.match(/\${(.*?)}/);
  return versionLinkMatch ? allVersions[versionLinkMatch[1]] : valueToResolve;
};
const distributionProperties = Object.fromEntries(
  Object.entries(jsonPom.project.properties).map(([key, value]) => [key, resolveLinkedVersion(jsonPom.project.properties, value)]),
);
const distributionDependencies = jsonPom.project.dependencies.dependency.map((dependency) => ({
  ...dependency,
  version: resolveLinkedVersion(
    {
      ...distributionProperties,
      'project.version': releasingVersion,
    },
    dependency.version,
  ),
}));

console.log(chalk.blue(`Step 2: Download all dependencies from artifactory`));
await $`mkdir -p ${tmpPath}`;
cd(tmpPath);

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
  {
    groupId: 'io.gravitee.apim.repository',
    artifactId: 'gravitee-apim-repository-hazelcast',
    version: releasingVersion,
    type: 'zip',
  },
  {
    groupId: 'io.gravitee.apim.repository',
    artifactId: 'gravitee-apim-repository-redis',
    version: releasingVersion,
    type: 'zip',
  },
].map((dependency) => {
  const fileName = `${dependency.artifactId}-${dependency.version}.${dependency.type}`;
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

await spinner('Download all dependencies...', () =>
  Promise.all(
    allDependencies.map(async ({ downloadUrl }) => {
      try {
        await $`curl -u ${process.env.ARTIFACTORY_USERNAME}:${process.env.ARTIFACTORY_PASSWORD} -f -O ${downloadUrl}`;
        return Promise.resolve(downloadUrl);
      } catch (e) {
        console.log(chalk.red(`Failed to download ${downloadUrl}`));
        return Promise.reject();
      }
    }),
  ),
);

console.log(chalk.green(`\n   ${allDependencies.length} dependencies downloaded`));

console.log(chalk.blue(`Step 3; Packaging - Components / Rest API`));
const restApiComponentDir = `./dist/components/gravitee-management-rest-api`;
await $`rm -rf ${restApiComponentDir} && mkdir -p ${restApiComponentDir}`;

await $`unzip -q -o ${allDependenciesMap.get('gravitee-apim-rest-api-standalone-distribution-zip').fileName} -d ${restApiComponentDir}`;
await $`mv ${restApiComponentDir}/gravitee-apim-rest-api-standalone-${releasingVersion} ${restApiComponentDir}/gravitee-apim-rest-api-${releasingVersion}`;

const restApiDependenciesExclusion = [
  // GroupIds to exclude
  'io.gravitee.apim.ui',
  'io.gravitee.apim.gateway.standalone.distribution',
  'io.gravitee.apim.rest.api.standalone.distribution',
  'io.gravitee.apim.repository.gateway.bridge.http',
  'io.gravitee.reporter',
  'io.gravitee.tracer',
  // ArtifactIds to exclude
  'gravitee-gateway-services-ratelimit',
  'gravitee-apim-repository-hazelcast',
  'gravitee-apim-repository-redis',
];
await spinner('Add plugins to Rest API...', () =>
  Promise.all(
    allDependencies
      .filter((d) => !restApiDependenciesExclusion.includes(d.artifactId) && !restApiDependenciesExclusion.includes(d.groupId))
      .map(({ fileName }) => $`cp ${fileName} ${restApiComponentDir}/gravitee-apim-rest-api-${releasingVersion}/plugins`),
  ),
);

await within(async () => {
  cd(`${restApiComponentDir}`);
  await $`zip -q -r gravitee-apim-rest-api-${releasingVersion}.zip gravitee-apim-rest-api-${releasingVersion}`;
  await createFileSum(`gravitee-apim-rest-api-${releasingVersion}.zip`);
});

console.log(chalk.blue(`Step 4: Packaging - Components / Gateway`));
const gatewayComponentDir = `./dist/components/gravitee-gateway`;
await $`rm -rf ${gatewayComponentDir} && mkdir -p ${gatewayComponentDir}`;

await $`unzip -q -o ${allDependenciesMap.get('gravitee-apim-gateway-standalone-distribution-zip').fileName} -d ${gatewayComponentDir}`;
await $`mv ${gatewayComponentDir}/gravitee-apim-gateway-standalone-${releasingVersion} ${gatewayComponentDir}/gravitee-apim-gateway-${releasingVersion}`;

const gatewayDependenciesExclusion = [
  // GroupIds to exclude
  'io.gravitee.apim.ui',
  'io.gravitee.apim.gateway.standalone.distribution',
  'io.gravitee.apim.rest.api.standalone.distribution',
  'io.gravitee.cockpit',
  'io.gravitee.fetcher',
  'io.gravitee.notifier',
  'io.gravitee.tracer',
  // ArtifactIds to exclude
  'gravitee-apim-repository-elasticsearch',
  'gravitee-apim-repository-hazelcast',
  'gravitee-apim-repository-redis',
];
await spinner('Add plugins to Gateway...', () =>
  Promise.all(
    allDependencies
      .filter((d) => !gatewayDependenciesExclusion.includes(d.artifactId) && !gatewayDependenciesExclusion.includes(d.groupId))
      .map(async ({ fileName }) => $`cp ${fileName} ${gatewayComponentDir}/gravitee-apim-gateway-${releasingVersion}/plugins`),
  ),
);

await within(async () => {
  cd(`${gatewayComponentDir}`);
  await $`zip -q -r gravitee-apim-gateway-${releasingVersion}.zip gravitee-apim-gateway-${releasingVersion}`;
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
const fullDistributionDir = `./dist/distributions/graviteeio-full-${releasingVersion}`;
await $`rm -rf ${fullDistributionDir} && mkdir -p ${fullDistributionDir}`;

await $`unzip -q -o ${allDependenciesMap.get('gravitee-apim-console-webui').fileName} -d ${fullDistributionDir}`;
await $`unzip -q -o ${allDependenciesMap.get('gravitee-apim-portal-webui').fileName} -d ${fullDistributionDir}`;

await $`mv -f ${restApiComponentDir}/gravitee-apim-rest-api-${releasingVersion} ./${fullDistributionDir}/`;
await $`mv -f ${gatewayComponentDir}/gravitee-apim-gateway-${releasingVersion} ./${fullDistributionDir}/`;

// TODO: Remove the following lines to align names of these components to match the naming convention `gravitee-*` and not `graviteeio-*`
await $`mv ${fullDistributionDir}/gravitee-apim-console-webui-${releasingVersion} ${fullDistributionDir}/graviteeio-apim-console-webui-${releasingVersion}`;
await $`mv ${fullDistributionDir}/gravitee-apim-portal-webui-${releasingVersion} ${fullDistributionDir}/graviteeio-apim-portal-webui-${releasingVersion}`;
await $`mv ${fullDistributionDir}/gravitee-apim-gateway-${releasingVersion} ${fullDistributionDir}/graviteeio-apim-gateway-${releasingVersion}`;
await $`mv ${fullDistributionDir}/gravitee-apim-rest-api-${releasingVersion} ${fullDistributionDir}/graviteeio-apim-rest-api-${releasingVersion}`;

await within(async () => {
  cd(`./dist/distributions`);
  await $`zip -q -r graviteeio-full-${releasingVersion}.zip graviteeio-full-${releasingVersion}`;
  await createFileSum(`graviteeio-full-${releasingVersion}.zip`);
});

await $`rm -rf ${fullDistributionDir}`;

console.log(chalk.blue(`Step 8: Packaging - Plugins / All Repositories`));
const repositoriesPluginDir = `./dist/plugins/repositories`;
await $`rm -rf ${repositoriesPluginDir} && mkdir -p ${repositoriesPluginDir}`;

await Promise.all(
  allDependencies
    .filter((d) => d.groupId.startsWith('io.gravitee.apim.repository'))
    .map(async ({ fileName, artifactId }) => {
      await $`mkdir -p  ${repositoriesPluginDir}/${artifactId} && cp ${fileName} ${repositoriesPluginDir}/${artifactId}/`;
      await createFileSum(`${repositoriesPluginDir}/${artifactId}/${fileName}`);
    }),
);

console.log(chalk.magenta(`📦 The package is ready!`));
