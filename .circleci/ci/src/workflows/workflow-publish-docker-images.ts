import { Config, workflow, Workflow } from '@circleci/circleci-config-sdk';
import { BuildBackendImagesJob, BuildBackendJob, SetupJob } from '../jobs';
import { config } from '../config';
import { CircleCIEnvironment } from '../pipelines';
import { WebuiBuildJob } from '../jobs';

export class PublishDockerImagesWorkflow {
  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const setupJob = SetupJob.create(dynamicConfig);
    dynamicConfig.addJob(setupJob);

    const buildBackendJob = BuildBackendJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(buildBackendJob);

    const buildBackendImagesJob = BuildBackendImagesJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(buildBackendImagesJob);

    const webuiBuildJob = WebuiBuildJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(webuiBuildJob);

    const jobs = [
      new workflow.WorkflowJob(setupJob, { context: config.jobContext, name: 'Setup' }),
      new workflow.WorkflowJob(buildBackendJob, { context: config.jobContext, requires: ['Setup'], name: 'Build backend' }),
      new workflow.WorkflowJob(buildBackendImagesJob, {
        context: config.jobContext,
        requires: ['Build backend'],
        name: 'Build and push rest api and gateway images',
      }),
      new workflow.WorkflowJob(webuiBuildJob, {
        context: config.jobContext,
        requires: ['Setup'],
        name: 'Build APIM Console and publish image',
        'apim-ui-project': 'gravitee-apim-console-webui',
        'docker-image-name': 'apim-management-ui',
      }),
      new workflow.WorkflowJob(webuiBuildJob, {
        context: config.jobContext,
        requires: ['Setup'],
        name: 'Build APIM Portal and publish image',
        'apim-ui-project': 'gravitee-apim-portal-webui',
        'docker-image-name': 'apim-portal-ui',
      }),
    ];

    return new Workflow('publish_docker_images', jobs);
  }
}
