import { Config, workflow, Workflow } from '@circleci/circleci-config-sdk';
import { BuildBackendJob, SetupJob } from '../jobs';
import { config } from '../config';
import { CircleCIEnvironment } from '../pipelines';

export class PublishDockerImagesWorkflow {
  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const setupJob = SetupJob.create(dynamicConfig);
    dynamicConfig.addJob(setupJob);

    const buildBackendJob = BuildBackendJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(buildBackendJob);

    const jobs = [
      new workflow.WorkflowJob(setupJob, { context: config.jobContext, name: 'Setup' }),
      new workflow.WorkflowJob(buildBackendJob, { context: config.jobContext, requires: ['Setup'] }),
    ];

    return new Workflow('publish_docker_images', jobs);
  }
}
