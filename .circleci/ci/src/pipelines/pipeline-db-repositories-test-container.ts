import { Config } from '@circleci/circleci-config-sdk';
import { CircleCIEnvironment } from './circleci-environment';
import { DbRepositoriesTestContainerWorkflow } from '../workflows';

export function generateDbRepositoriesTestContainerConfig(environment: CircleCIEnvironment): Config {
  const dynamicConfig = new Config();
  const workflow = DbRepositoriesTestContainerWorkflow.create(dynamicConfig, environment);
  dynamicConfig.addWorkflow(workflow);
  return dynamicConfig;
}
