import { Config, Workflow, workflow } from '@circleci/circleci-config-sdk';
import { CircleCIEnvironment } from '../pipelines';
import { AddDockerImagesInSnykJob, PublishProdDockerImagesJob, PublishRpmPackagesJob } from '../jobs';
import { config } from '../config';

export class BuildRpmAndDockerImagesWorkflow {
  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const publishProdDockerImagesJob = PublishProdDockerImagesJob.create(dynamicConfig, environment);
    const addDockerImagesInSnykJob = AddDockerImagesInSnykJob.create(dynamicConfig, environment);
    const publishRpmPackagesJob = PublishRpmPackagesJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(publishProdDockerImagesJob);
    dynamicConfig.addJob(addDockerImagesInSnykJob);
    dynamicConfig.addJob(publishRpmPackagesJob);

    let publishProdDockerImagesJobName = `Build and push docker images for APIM ${environment.graviteeioVersion}`;
    let publishRpmPackagesJobName = `Build and push RPM packages for APIM ${environment.graviteeioVersion}`;

    if (environment.isDryRun) {
      publishProdDockerImagesJobName += ' - Dry Run';
      publishRpmPackagesJobName += ' - Dry Run';
    }

    const jobs = [
      new workflow.WorkflowJob(publishProdDockerImagesJob, {
        context: config.jobContext,
        name: publishProdDockerImagesJobName,
      }),
      new workflow.WorkflowJob(addDockerImagesInSnykJob, {
        context: config.jobContext,
        name: 'Add images to Snyk',
        requires: [publishProdDockerImagesJobName],
      }),
      new workflow.WorkflowJob(publishRpmPackagesJob, {
        context: config.jobContext,
        name: publishRpmPackagesJobName,
      }),
    ];

    return new Workflow('build-rpm-&-docker-images', jobs);
  }
}
