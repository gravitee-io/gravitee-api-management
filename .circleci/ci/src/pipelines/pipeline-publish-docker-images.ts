import { Config } from '@circleci/circleci-config-sdk';
import { PublishDockerImagesWorkflow } from '../workflows';

export function generatePublishDockerImagesConfig(): Config {
  const dynamicConfig = new Config();
  const workflow = PublishDockerImagesWorkflow.create(dynamicConfig);
  dynamicConfig.addWorkflow(workflow);
  return dynamicConfig;
}
