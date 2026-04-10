/**
 * CircleCI environment context — passed to pipeline builders.
 */
export interface CircleCIEnvironment {
  baseBranch: string;
  branch: string;
  buildNum: string;
  buildId: string;
  sha1: string;
  action: string;
  isDryRun: boolean;
  dockerTagAsLatest?: boolean;
  graviteeioVersion: string;
  changedFiles: string[];
  apimVersionPath: string;
}
