import { Config } from '@circleci/circleci-config-sdk';
import { PackageBundleWorkflow } from '../workflows';
import { validateGraviteeioVersion } from '../utils';
import { CircleCIEnvironment } from './circleci-environment';

export function generatePackageBundleConfig(environment: CircleCIEnvironment): Config {
  validateGraviteeioVersion(environment.graviteeioVersion);

  const dynamicConfig = new Config();
  const workflow = PackageBundleWorkflow.create(dynamicConfig, environment.graviteeioVersion, environment.isDryRun);
  dynamicConfig.addWorkflow(workflow);
  return dynamicConfig;
}
