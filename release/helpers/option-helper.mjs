export function isDryRun() {
  return !!argv['dry-run'];
}

/**
 * Target branch of the pipeline: --branch if provided, otherwise the branch derived from the version.
 * @param {{branch: string}} versions result of computeVersion()
 * @returns {string}
 */
export function getTargetBranch(versions) {
  return argv.branch ?? versions.branch;
}
