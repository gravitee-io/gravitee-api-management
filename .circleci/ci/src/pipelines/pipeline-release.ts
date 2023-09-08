import { Config } from '@circleci/circleci-config-sdk';
import { ReleaseWorkflow } from '../workflows';
import { CircleCIEnvironment } from './circleci-environment';
import { isSupportBranch } from '../utils';

export function generateReleaseConfig(environment: CircleCIEnvironment): Config {
  if (!isSupportBranch(environment.branch)) {
    throw new Error('Release is only supported on support branches');
  }

  const dynamicConfig = new Config();
  const workflow = ReleaseWorkflow.create(dynamicConfig, environment);
  dynamicConfig.addWorkflow(workflow);
  return dynamicConfig;
}
