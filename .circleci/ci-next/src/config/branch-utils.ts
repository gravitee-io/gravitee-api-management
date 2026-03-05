/**
 * Branch detection utilities.
 */

export function sanitizeBranch(branch: string): string {
  return branch
    .replaceAll(/[~^]+/g, '')
    .replaceAll(/[^a-zA-Z0-9\\.]+/g, '-')
    .replaceAll(/^-+|-+$/g, '')
    .toLowerCase()
    .substring(0, 60);
}

export function isE2EBranch(branch: string): boolean {
  return /.*-run-e2e.*/.test(branch);
}

export function isMasterBranch(branch: string): boolean {
  return branch === 'master';
}

export function isSupportBranch(branch: string): boolean {
  return /^\d+\.\d+\.x$/.test(branch);
}

export function isSupportBranchOrMaster(branch: string): boolean {
  return isMasterBranch(branch) || isSupportBranch(branch);
}

export function computeImagesTag(branch: string, sha1?: string): string {
  const sanitized = sanitizeBranch(branch);
  return sha1 ? `${sanitized}-${sha1.substring(0, 8)}` : `${sanitized}-latest`;
}
