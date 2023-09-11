import { Config } from '@circleci/circleci-config-sdk';
import { CircleCIEnvironment } from './circleci-environment';
import { RepositoriesTestsWorkflow } from '../workflows';

export function generateRepositoriesTestsConfig(environment: CircleCIEnvironment): Config {
  const dynamicConfig = new Config();
  const workflow = RepositoriesTestsWorkflow.create(dynamicConfig, environment);
  dynamicConfig.addWorkflow(workflow);
  return dynamicConfig;
}
