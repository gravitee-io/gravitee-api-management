#!/usr/bin/env zx

import { checkToken } from '../helpers/circleci-helper.mjs';
import { computeVersion, extractVersion } from '../helpers/version-helper.mjs';
import { isDryRun } from '../helpers/option-helper.mjs';
import { waitWorkflowSuccessPromise } from '../helpers/fetch-helper.mjs';

await checkToken();

const releasingVersion = await extractVersion();
const versions = computeVersion(releasingVersion);

/**
 * Search in gravitee-io/gravitee-docs if there is a PR containing the version to release in its title and that should be closed
 * @returns {Promise<string>} a formatted string of the pull requests related to the version
 */
async function searchExistingDocumentationPulRequests() {
  const githubDocumentationQuery = `${releasingVersion} is:pr repo:gravitee-io/gravitee-docs base:master`;
  const docsPullRequestsResponse = await fetch(`https://api.github.com/search/issues?q=${encodeURIComponent(githubDocumentationQuery)}`, {
    method: 'get',
    headers: {
      Accept: 'application/vnd.github+json',
    },
  });

  const docsPrsData = await docsPullRequestsResponse.json();
  const docPrs = docsPrsData.items.map((item) => item['pull_request'].html_url);

  let docPrsText = docPrs.length > 0 ? 'Some PRs found in gravitee-docs 👀:\n' : '';
  docPrs.forEach((pr) => (docPrsText += `\t* ${pr}\n`));
  return docPrsText;
}

console.log(chalk.blue(`Triggering Nexus Sync Pipeline`));

// Use the preconfigured payload from config folder with the good parameters
const body = {
  branch: versions.branch,
  parameters: {
    gio_action: 'nexus_staging',
    dry_run: isDryRun(),
    graviteeio_version: releasingVersion,
  },
};

const docPullRequests = await searchExistingDocumentationPulRequests();

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

console.log(chalk.green(`Nexus Sync Pipeline started !`));
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

console.log(chalk.green(`Step 5/5 finished`));

console.log(
  chalk.greenBright(
    'Do not forget to:\n' +
      `- merge gravitee-docs PR if exists for ${releasingVersion}\n` +
      docPullRequests +
      'Release is done! Congrats 🏆',
  ),
);
