export const supportPattern = /^\d+\.\d+\.x$/;

export function sanitizeBranch(branch: string) {
  return branch
    .replaceAll(/[~^]+/g, '')
    .replaceAll(/[^a-zA-Z0-9\\.]+/g, '-')
    .replaceAll(/^-+|-+$/g, '')
    .toLowerCase()
    .substring(0, 60);
}

export function isMasterBranch(branch: string) {
  return branch === 'master';
}

export function isSupportBranch(branch: string): boolean {
  return supportPattern.test(branch);
}
