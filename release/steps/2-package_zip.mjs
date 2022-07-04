#!/usr/bin/env zx

import { checkToken } from '../helpers/circleci-helper.mjs';
import { computeVersion, extractVersion } from '../helpers/version-helper.mjs';
import { isDryRun } from '../helpers/option-helper.mjs';
const xml2json = require('xml2json');

await checkToken();

const releasingVersion = await extractVersion();
const versions = computeVersion(releasingVersion);

console.log(chalk.blue(`Triggering Package Zip Pipeline`));

const tagSteps = await question(
  chalk.blue(
    '⚠️ Before packaging, you have to:\n' +
      " - Update gravitee-io/release's release.json to update 'gravitee-api-management' version\n" +
      ' - Commit\n' +
      " - Create a tag for the new version (To create a tag, use 'git tag -a {version}' then 'git push origin {version}')\n" +
      '\n' +
      'Is it ok ? (y/n)\n',
  ),
);
if (tagSteps !== 'y') {
  console.log(chalk.yellow('Follow previous steps and try again!'));
  process.exit();
}

// Use the preconfigured payload from config folder with the good parameters
const body = {
  branch: versions.branch,
  parameters: {
    gio_action: 'package_bundle',
    dry_run: isDryRun(),
    graviteeio_version: releasingVersion,
  },
};

// Get EE versions from distribution pom
const allProperties = await propertiesFromDistributionPom();
const ee_versions = {
  ae_version: allProperties['gravitee-ae-connectors-ws.version'],
  license_version: allProperties['gravitee-license-node.version'],
  notifier_slack_version: allProperties['gravitee-notifier-slack.version'],
  notifier_webhook_version: allProperties['gravitee-notifier-webhook.version'],
  notifier_email_version: allProperties['gravitee-notifier-email.version'],
};

// Merge EE versions into body parameters
body.parameters = {
  ...body.parameters,
  ...ee_versions,
};

console.log(`Here are the EE versions to package with APIM ${releasingVersion}: `, ee_versions);

const eeResponse = await question(chalk.blue('Are EE good versions good? (y/n)\n'));
if (eeResponse !== 'y') {
  console.log(chalk.yellow(`Please update 'gravitee-apim-distribution/pom.xml' with the good versions`));
  process.exit();
}

const response = await fetch('https://circleci.com/api/v2/project/gh/gravitee-io/gravitee-api-management/pipeline', {
  method: 'post',
  body: JSON.stringify(body),
  headers: {
    'Content-Type': 'application/json',
    'Circle-Token': process.env.CIRCLECI_TOKEN,
  },
});

const data = await response.json();

if (response.status === 201) {
  console.log(chalk.green(`Pipeline created with number: ${data.number}`));
  echo`Follow its progress on: https://app.circleci.com/pipelines/github/gravitee-io/gravitee-api-management/${data.number}`;
  console.log(chalk.greenBright(`When it's done, run 'npm run docker_rpms -- --version=${releasingVersion}'`));
} else {
  console.log(chalk.yellow('Something went wrong'));
}

/**
 * Extract plugin properties from distribution pom
 * @returns {Promise<PropertyPreview[]>}
 */
async function propertiesFromDistributionPom() {
  // Read and parse pom as a json
  const pomXml = await fs.readFile(`../gravitee-apim-distribution/pom.xml`, 'utf-8');
  const jsonPom = JSON.parse(await xml2json.toJson(pomXml, {}));

  // For versions before 3.18 and before merge of CE and EE bundles, ee properties where in a dedicated profile
  const eeProperties = jsonPom.project.profiles.profile.find((p) => p.id === 'distribution-ee');
  let props = jsonPom.project.properties;

  if (eeProperties) {
    props = {
      ...props,
      ...eeProperties.properties,
    };
  }
  return props;
}
