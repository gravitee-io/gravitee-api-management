export type CircleCIEnvironment = {
  branch: string;
  sha1: string;
  action: string;
  isDryRun: boolean;
  dockerTagAsLatest?: boolean;
  graviteeioVersion?: string | undefined;

  changedFiles: string[];
};
