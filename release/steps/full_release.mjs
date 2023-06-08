#!/usr/bin/env zx

import { checkToken } from '../helpers/circleci-helper.mjs';
import { computeVersion, extractVersion } from '../helpers/version-helper.mjs';
import { isDryRun } from '../helpers/option-helper.mjs';

await checkToken();

const releasingVersion = await extractVersion();
const versions = computeVersion(releasingVersion);

console.log(chalk.green(`💪 Triggering Release Pipeline!\n`));

const isReadyToRelease = await question(chalk.blue(`📝 Ensure Release list is good for ${releasingVersion} in JIRA. Should we continue? (y/n)\n`));
if (isReadyToRelease === 'n') {
  try {
    await $`exit 1`;
  } catch (p) {
    console.log(chalk.yellow(`🚦 Release process interrupted. Verify JIRA release for ${releasingVersion} and try again!`))
  }
}

let isLatest = false;
if (argv.latest) {
  isLatest = true;
} else {
  const shouldBeLatest = await question(chalk.blue(`⚠️ No '--latest' parameter. Should this version be flagged as 'latest'? (y/n)\n`));
  if (shouldBeLatest === 'y') {
    isLatest = true;
  }
}

const body = {
  branch: versions.branch,
  parameters: {
    gio_action: 'full_release',
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

if (response.status === 201) {
  console.log(chalk.green(`Pipeline created with number: ${data.number}`));
  echo`Follow its progress on: https://app.circleci.com/pipelines/github/gravitee-io/gravitee-api-management/${data.number}`;

  console.log(
      chalk.greenBright(
          'Just wait for the end of the workflow and release is done! Congrats 🏆\n' +
          'When it\'s done, do not forget to:\n' +
          `- merge gravitee-docs PR if it exists for ${releasingVersion}`
      ),
  );
} else {
  console.log(chalk.yellow('Something went wrong'));
}