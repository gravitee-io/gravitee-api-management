import { Config, workflow, Workflow } from '@circleci/circleci-config-sdk';
import { SetupJob } from '../jobs';
import { config } from '../config';

export class PublishDockerImagesWorkflow {
  static create(dynamicConfig: Config) {
    const setupJob = SetupJob.create(dynamicConfig);
    dynamicConfig.addJob(setupJob);

    const jobs = [new workflow.WorkflowJob(setupJob, { context: config.jobContext })];

    return new Workflow('publish_docker_images', jobs);
  }
}
