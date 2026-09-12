#!/usr/bin/env zx

import { checkToken, triggerPipeline } from '../helpers/circleci-helper.mjs';
import {
  assertChartMatchesVersion,
  assertTagIsFree,
  assertVersionMatchesPoms,
  computeVersion,
  DISTRIBUTION_POM,
  extractVersion,
} from '../helpers/version-helper.mjs';
import { announceMode, confirm, getTargetBranch, isDryRun } from '../helpers/option-helper.mjs';

await checkToken();

const releasingVersion = await extractVersion();
const versions = computeVersion(releasingVersion);
const targetBranch = getTargetBranch(versions);
const dryRun = isDryRun();

// Read-only, and run here rather than in CI: they refuse in a second instead of after a pipeline
// has spun up, and they refuse before anything has been written. The root pom is not among them —
// the core lane releases it under its own tag, and the two versions no longer have to agree.
await assertVersionMatchesPoms(releasingVersion, targetBranch, [DISTRIBUTION_POM]);
await assertChartMatchesVersion(releasingVersion, targetBranch);
await assertTagIsFree(releasingVersion);

console.log(chalk.green(`💪 Preparing the distribution release of ${releasingVersion}\n`));
announceMode(dryRun);
console.log(chalk.blue(`Branch: ${targetBranch}`));
console.log(chalk.blue(`Tag: ${releasingVersion}\n`));
console.log(`CI will commit ${releasingVersion}, tag it, and reopen ${targetBranch} on the next version.`);
console.log(`Pushing that tag is what publishes the product — the branch is pushed first.\n`);

await confirm(`Should we continue?`);

await triggerPipeline(targetBranch, {
  gio_action: 'prepare_distribution_release',
  dry_run: dryRun,
  graviteeio_version: releasingVersion,
});
