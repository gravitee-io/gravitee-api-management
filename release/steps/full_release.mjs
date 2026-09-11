#!/usr/bin/env zx

import { checkToken } from '../helpers/circleci-helper.mjs';
import { assertVersionMatchesPoms, computeVersion, DISTRIBUTION_POM, extractVersion, ROOT_POM } from '../helpers/version-helper.mjs';
import { announceMode, confirm, isDryRun, getTargetBranch } from '../helpers/option-helper.mjs';

await checkToken();

const releasingVersion = await extractVersion();
const versions = computeVersion(releasingVersion);
const targetBranch = getTargetBranch(versions);
await assertVersionMatchesPoms(releasingVersion, targetBranch, [ROOT_POM, DISTRIBUTION_POM]);

const dryRun = isDryRun();

console.log(chalk.green(`💪 Triggering Release Pipeline!\n`));
announceMode(dryRun);

await confirm(
  `📝 Ensure Release list is good for ${releasingVersion} in JIRA. Should we continue?`,
  `🚦 Release process interrupted. Verify JIRA release for ${releasingVersion} and try again!`,
);

await confirm(
  `📝 Ensure you have removed all alpha versions if needed (Helm Chart, pom.xml). Should we continue?`,
  `🚦 Release process interrupted. Remove alpha versions and try again!`,
);

let isLatest = false;
if (argv.latest) {
  isLatest = true;
} else {
  const shouldBeLatest = await question(chalk.blue(`⚠️ No '--latest' parameter. Should this version be flagged as 'latest'? (y/n)\n`));
  if (shouldBeLatest === 'y') {
    isLatest = true;
  }
}

if (argv.branch && argv.branch !== versions.branch) {
  await confirm(
    `⚠️ Releasing ${releasingVersion} from non-default branch '${targetBranch}'. The version bump commit and tag will be pushed there. Should we continue?`,
    `🚦 Release process interrupted. Re-run with the right '--branch' (or none) and try again!`,
  );
}

console.log(chalk.blue(`Version: ${releasingVersion}`));
console.log(chalk.blue(`Branch: ${targetBranch}`));
console.log(chalk.blue(`Docker 'latest': ${isLatest}\n`));

// The last gate before the POST. The questions above are checklist reminders; this one is the
// moment the release becomes real, and until now nothing stood here at all.
await confirm(`Trigger this release?`);

const body = {
  branch: targetBranch,
  parameters: {
    gio_action: 'full_release',
    docker_tag_as_latest: isLatest,
    dry_run: dryRun,
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
        "When it's done, do not forget to:\n" +
        `- merge gravitee-docs PR if it exists for ${releasingVersion}`,
    ),
  );
} else {
  console.log(chalk.yellow('Something went wrong'));
}
