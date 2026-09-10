#!/usr/bin/env zx

import { checkToken, triggerPipeline } from '../helpers/circleci-helper.mjs';
import { assertCoreTagIsFree, assertVersionMatchesPoms, computeVersion, extractVersion, ROOT_POM } from '../helpers/version-helper.mjs';
import { getTargetBranch, isDryRun, isHotfixVersion } from '../helpers/option-helper.mjs';

await checkToken();

const releasingVersion = await extractVersion();
const versions = computeVersion(releasingVersion);
const targetBranch = getTargetBranch(versions);
const dryRun = isDryRun();

// A hotfix version is released from a branch cut off a tag, and the pinning job derives its target
// from the version — so it would open the pull request on the support line, the one branch that must
// not receive this core, while the hotfix branch that needs it got nothing. Refused rather than
// guessed until BX-394 gives the hotfix lane its own shape.
if (isHotfixVersion(releasingVersion)) {
  console.log(chalk.red(`${releasingVersion} is a hotfix version, and the core lane cannot pin it yet.`));
  console.log(`See https://gravitee.atlassian.net/browse/BX-394.`);
  process.exit(1);
}

// Read-only, and run here rather than in CI: they refuse in a second instead of after a pipeline
// has spun up, and they refuse before anything has been written.
await assertVersionMatchesPoms(releasingVersion, targetBranch, [ROOT_POM]);
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
