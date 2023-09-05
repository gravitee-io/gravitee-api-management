import { Config } from '@circleci/circleci-config-sdk';
import { PublishDockerImagesWorkflow } from '../workflows';
import { CircleCIEnvironment } from './circleci-environment';

export function generatePublishDockerImagesConfig(environment: CircleCIEnvironment): Config {
  const dynamicConfig = new Config();
  const workflow = PublishDockerImagesWorkflow.create(dynamicConfig, environment);
  dynamicConfig.addWorkflow(workflow);
  return dynamicConfig;
}
