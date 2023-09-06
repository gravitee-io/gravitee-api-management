import { CircleCIEnvironment } from './circleci-environment';
import { Config } from '@circleci/circleci-config-sdk';
import { ReleaseNotesApimWorkflow } from '../workflows';
import { validateGraviteeioVersion } from '../utils';

export function generateReleaseNotesApimConfig(environment: CircleCIEnvironment): Config {
  validateGraviteeioVersion(environment.graviteeioVersion);

  const dynamicConfig = new Config();
  const workflow = ReleaseNotesApimWorkflow.create(dynamicConfig, environment);
  dynamicConfig.addWorkflow(workflow);
  return dynamicConfig;
}
