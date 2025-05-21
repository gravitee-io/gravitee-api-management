#!/usr/bin/env zx

import { checkToken } from '../helpers/circleci-helper.mjs';
import { computeVersion, extractVersion } from '../helpers/version-helper.mjs';
import { isDryRun } from '../helpers/option-helper.mjs';

await checkToken();

const releasingVersion = await extractVersion();
const versions = computeVersion(releasingVersion);

console.log(chalk.blue(`Triggering Release Notes generation Pipeline`));

// Use the preconfigured payload from config folder with the good parameters
// For release-notes generation, always work on master
const body = {
  branch: versions.branch,
  parameters: {
    gio_action: 'release_notes_apim',
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
  console.log(chalk.greenBright(`When it's done, run 'yarn nexus_sync --version=${releasingVersion}'`));
} else {
  console.log(chalk.yellow('Something went wrong'));
}
