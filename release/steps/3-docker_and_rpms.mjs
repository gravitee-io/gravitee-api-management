#!/usr/bin/env zx

import { checkToken } from '../helpers/circleci-helper.mjs';
import { computeVersion, extractVersion } from '../helpers/version-helper.mjs';
import { isDryRun } from '../helpers/option-helper.mjs';
import { waitWorkflowSuccessPromise } from '../helpers/fetch-helper.mjs';

await checkToken();

const releasingVersion = await extractVersion();
const versions = computeVersion(releasingVersion);

let isLatest = false;
if (argv.latest) {
  isLatest = true;
} else {
  const shouldBeLatest = await question(chalk.blue(`No '--latest' parameter. Should this version be flagged as 'latest'? (y/n)\n`));
  if (shouldBeLatest === 'y') {
    isLatest = true;
  }
}

console.log(chalk.blue(`Triggering Docker & RPMs Pipeline`));

// Use the preconfigured payload from config folder with the good parameters
const body = {
  branch: versions.branch,
  parameters: {
    gio_action: 'build_rpm_&_docker_images',
    docker_tag_as_latest: isLatest,
    dry_run: isDryRun(),
    graviteeio_version: releasingVersion,
  },
};

const response = await fetch('https://circleci.com/api/v2/project/gh/gravitee-io/gravitee-api-management/pipeline', {
  method: 'post',
  body: JSON.stringify(body),
  headers: {
    'Content-Type': 'application/json',
    'Circle-Token': process.env.CIRCLECI_TOKEN,
  },
});

const data = await response.json();
if (response.status !== 201) {
  console.log(chalk.red('Something went wrong'));
  process.exit(1);
}

console.log(chalk.green(`Docker & RPMs Pipeline started !`));
echo`Progress on CircleCi: https://app.circleci.com/pipelines/github/gravitee-io/gravitee-api-management/${data.number}`;

$.verbose = false;
let isFinished = await spinner('Progress in progress...', () =>
  waitWorkflowSuccessPromise(data.id)
    .then((_) => true)
    .catch((_) => false),
);
$.verbose = true;

if (!isFinished) {
  console.log(chalk.red('Something went wrong'));
  process.exit(1);
}

console.log(chalk.green(`Step 3/5 finished`));

const next = await question(
  chalk.blue(`Continue and run next step command 'npm run release_notes -- --version=${releasingVersion}'? (y/n)`),
).then((answer) => (answer === 'y' ? true : false));
if (!next) {
  process.exit(1);
}

await $`npm run release_notes -- --version=${releasingVersion}`;
