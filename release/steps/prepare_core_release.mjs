#!/usr/bin/env zx

import { checkToken, triggerPipeline } from '../helpers/circleci-helper.mjs';
import { assertCoreTagIsFree, assertVersionMatchesPom, computeVersion, extractVersion } from '../helpers/version-helper.mjs';
import { getTargetBranch, isDryRun } from '../helpers/option-helper.mjs';

await checkToken();

const releasingVersion = await extractVersion();
const versions = computeVersion(releasingVersion);
const targetBranch = getTargetBranch(versions);
const dryRun = isDryRun();

// Read-only, and run here rather than in CI: they refuse in a second instead of after a pipeline
// has spun up, and they refuse before anything has been written.
await assertVersionMatchesPom(releasingVersion, targetBranch);
await assertCoreTagIsFree(releasingVersion);

console.log(chalk.green(`💪 Preparing the core release of ${releasingVersion}${dryRun ? ' - Dry Run' : ''}\n`));
console.log(chalk.blue(`Branch: ${targetBranch}`));
console.log(chalk.blue(`Tag: core_${releasingVersion}\n`));
console.log(`CI will commit ${releasingVersion}, tag it, and reopen ${targetBranch} on the next version.`);
console.log(`Pushing that tag is what publishes the core — the branch is pushed first.\n`);

// Anything but a yes stops here. Testing for 'n' would let the empty answer through — the one a
// stray Enter produces — and this prompt guards a push that publishes.
const confirmed = await question(chalk.blue(`Should we continue? (y/n)\n`));
if (confirmed.trim().toLowerCase() !== 'y') {
  console.log(chalk.yellow(`🚦 Nothing was triggered.`));
  process.exit(1);
}

await triggerPipeline(targetBranch, {
  gio_action: 'prepare_core_release',
  dry_run: dryRun,
  graviteeio_version: releasingVersion,
});
