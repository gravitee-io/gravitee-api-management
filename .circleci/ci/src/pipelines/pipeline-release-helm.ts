import { Config } from '@circleci/circleci-config-sdk';
import { ReleaseHelmWorkflow } from '../workflows';
import { CircleCIEnvironment } from './circleci-environment';

export function generateReleaseHelmConfig(environment: CircleCIEnvironment): Config {
  const dynamicConfig = new Config();
  const workflow = ReleaseHelmWorkflow.create(dynamicConfig, environment);
  dynamicConfig.addWorkflow(workflow);
  return dynamicConfig;
}
