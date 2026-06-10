#!/usr/bin/env zx

import { checkToken } from '../helpers/circleci-helper.mjs';
import { computeVersion, extractVersion } from '../helpers/version-helper.mjs';
import { getTargetBranch } from '../helpers/option-helper.mjs';

await checkToken();

const releasingVersion = await extractVersion();
const versions = computeVersion(releasingVersion);

console.log(chalk.green(`💪 Triggering Maven Central Release Pipeline!\n`));

const hasRemovedAlpha = await question(
  chalk.blue(`📝 Ensure you have removed all alpha versions if needed (Helm Chart, pom.xml). Should we continue? (y/n)\n`),
);
if (hasRemovedAlpha === 'n') {
  try {
    await $`exit 1`;
  } catch (p) {
    console.log(chalk.yellow(`🚦 Release process interrupted. Remove alpha versions and try again!`));
  }
}

const targetBranch = getTargetBranch(versions);
if (argv.branch && argv.branch !== versions.branch) {
  const confirmBranch = await question(
    chalk.yellow(`⚠️ Releasing ${releasingVersion} from non-default branch '${targetBranch}'. Should we continue? (y/n)\n`),
  );
  if (confirmBranch === 'n') {
    console.log(chalk.yellow(`🚦 Release process interrupted. Re-run with the right '--branch' (or none) and try again!`));
    process.exit(1);
  }
}

console.log(chalk.blue(`Version: ${releasingVersion}`));
console.log(chalk.blue(`Branch: ${targetBranch}\n`));

const body = {
  branch: targetBranch,
  parameters: {
    gio_action: 'maven_release',
    dry_run: false,
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

  console.log(chalk.greenBright('Just wait for the end of the workflow and release is done! Congrats 🏆'));
} else {
  console.log(chalk.yellow('Something went wrong'));
  console.log(data);
}
