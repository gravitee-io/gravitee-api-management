import { Config } from '@circleci/circleci-config-sdk';
import { CircleCIEnvironment } from './circleci-environment';
import { PullRequestsWorkflow } from '../workflows';

export function generatePullRequestsConfig(environment: CircleCIEnvironment): Config {
  const dynamicConfig = new Config();
  const workflow = PullRequestsWorkflow.create(dynamicConfig, environment);
  dynamicConfig.addWorkflow(workflow);
  return dynamicConfig;
}
