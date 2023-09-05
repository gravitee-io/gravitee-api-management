import { sanitizeBranch } from './branch';

export function computeImagesTag(branch: string): string {
  return `${sanitizeBranch(branch)}-latest`;
}
