export type CircleCIEnvironment = {
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
};
