#!/usr/bin/env zx

console.log(chalk.magenta(`#########################################`));
console.log(chalk.magenta(`# 📦 Prepare APIM full distribution 📦 #`));
console.log(chalk.magenta(`#########################################`));

const releasingVersion = argv.version;
const tmpPath = `./.tmp/${releasingVersion}`;

await $`mkdir -p ${tmpPath}`;
cd(tmpPath);

const createFileSum = async (file) => {
  await $`md5sum ${file} > ${file}.md5`;
  await $`sha512sum ${file} > ${file}.sha512sum`;
  await $`sha1sum ${file} > ${file}.sha1`;
};

console.log(chalk.blue(`Packaging - Distribution / Full`));
const fullDistributionDir = `./dist/graviteeio-apim/distributions/graviteeio-full-${releasingVersion}/graviteeio-full-${releasingVersion}`;
await $`rm -rf ${fullDistributionDir} && mkdir -p ${fullDistributionDir}`;

// Console
await $`cp ../../../gravitee-apim-console-webui/dist ${fullDistributionDir}/graviteeio-apim-console-ui-${releasingVersion}`;

// Portal
await $`cp ../../../gravitee-apim-portal-webui/dist ${fullDistributionDir}/graviteeio-apim-portal-ui-${releasingVersion}`;

// Rest API
await $`cp ../../../gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target/distribution ${fullDistributionDir}/graviteeio-apim-rest-api-${releasingVersion}`;

// Gateway
await $`cp ../../../gravitee-apim-gateway/gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-distribution/target/distribution ${fullDistributionDir}/graviteeio-apim-gateway-${releasingVersion}`;

await within(async () => {
  cd(`${fullDistributionDir}`);
  cd(`../`);
  await $`zip -q -r ../graviteeio-full-${releasingVersion}.zip .`;
  cd(`../`);
  await createFileSum(`graviteeio-full-${releasingVersion}.zip`);
  await $`rm -rf graviteeio-full-${releasingVersion}`;
});

console.log(chalk.magenta(`📦 The full distribution ZIP is ready!`));
