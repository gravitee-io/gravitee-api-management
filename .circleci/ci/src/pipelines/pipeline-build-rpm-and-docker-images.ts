import { CircleCIEnvironment } from './circleci-environment';
import { Config } from '@circleci/circleci-config-sdk';
import { isBlank, validateGraviteeioVersion } from '../utils';
import { BuildRpmAndDockerImagesWorkflow } from '../workflows';

export function generateBuildRpmAndDockerImagesConfig(environment: CircleCIEnvironment): Config {
  validateGraviteeioVersion(environment.graviteeioVersion);

  if (isBlank(environment.branch)) {
    throw new Error('A branch (CIRCLE_BRANCH) must be specified');
  }

  const dynamicConfig = new Config();
  const workflow = BuildRpmAndDockerImagesWorkflow.create(dynamicConfig, environment);
  dynamicConfig.addWorkflow(workflow);
  return dynamicConfig;
}
