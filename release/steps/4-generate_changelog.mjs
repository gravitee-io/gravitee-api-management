#!/usr/bin/env zx

import { checkToken } from '../helpers/circleci-helper.mjs';
import { extractVersion } from '../helpers/version-helper.mjs';
import { isDryRun } from '../helpers/option-helper.mjs';

await checkToken();

const releasingVersion = await extractVersion();

console.log(chalk.blue(`Triggering Changelog generation Pipeline`));

const milestone = `APIM - ${releasingVersion}`;

// Use the preconfigured payload from config folder with the good parameters
// For changelog generation, always work on master
const body = {
  branch: 'master',
  parameters: {
    gio_action: 'changelog_apim',
    dry_run: isDryRun(),
    gio_milestone_version: milestone,
  },
};

const tagSteps = await question(
  chalk.blue(
    `Before generating the changelog, you have to ensure the milestone named '${chalk.bold(
      milestone,
    )}' is closed and all of its tickets too` +
      '\n' +
      'Is it ok ? (y/n)\n',
  ),
);
if (tagSteps !== 'y') {
  console.log(chalk.yellow('Please close the milestone and try again!'));
  process.exit();
}

const response = await fetch('https://circleci.com/api/v2/project/gh/gravitee-io/issues/pipeline', {
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
  echo`Follow its progress on: https://app.circleci.com/pipelines/github/gravitee-io/issues/${data.number}`;
  console.log(chalk.greenBright(`When it's done, run 'npm run nexus_sync -- --version=${releasingVersion}'`));
} else {
  console.log(chalk.yellow('Something went wrong'));
}
