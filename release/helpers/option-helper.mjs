export function isDryRun() {
  return !!argv['dry-run'];
}

const HOTFIX_QUALIFIER = /-hotfix\.\d+$/;

/**
 * Whether a version is released from a branch cut off a tag rather than from its support line.
 * @param {string} version
 * @returns {boolean}
 */
export function isHotfixVersion(version) {
  return HOTFIX_QUALIFIER.test(version);
}

/**
 * Branch the release runs on: the one --branch names, otherwise the one the version implies.
 *
 * That is not always versions.branch. A hotfix runs on the branch cut from the tag it fixes, while
 * versions.branch stays the support line the version belongs to — which is what the documentation
 * changelog is filed under, hotfix or not.
 * @param {{version: string, branch: string}} versions result of computeVersion()
 * @returns {string}
 */
export function getTargetBranch(versions) {
  const hotfixBranch = HOTFIX_QUALIFIER.test(versions.version) ? `hotfix/${versions.version.replace(HOTFIX_QUALIFIER, '')}` : undefined;

  if (hotfixBranch && argv.branch && argv.branch !== hotfixBranch) {
    console.log(chalk.red(`${versions.version} is released from ${hotfixBranch}, not from ${argv.branch}.`));
    process.exit(1);
  }

  return argv.branch ?? hotfixBranch ?? versions.branch;
}
