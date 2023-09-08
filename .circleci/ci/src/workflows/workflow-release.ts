import { Config, workflow, Workflow } from '@circleci/circleci-config-sdk';
import { ReleaseCommitAndPrepareNextVersionJob, SetupJob, SlackAnnouncementJob, WebuiBuildJob, WebuiPublishArtifactoryJob } from '../jobs';
import { CircleCIEnvironment } from '../pipelines';
import { config } from '../config';
import { BackendBuildAndPublishOnArtifactoryJob } from '../jobs/backend';

export class ReleaseWorkflow {
  private static workflowName = 'release';

  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const setupJob = SetupJob.create(dynamicConfig);
    dynamicConfig.addJob(setupJob);

    const slackAnnouncementJob = SlackAnnouncementJob.create(dynamicConfig);
    dynamicConfig.addJob(slackAnnouncementJob);

    const webuiBuildJob = WebuiBuildJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(webuiBuildJob);

    const webuiPublishArtifactoryJob = WebuiPublishArtifactoryJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(webuiPublishArtifactoryJob);

    const backendBuildAndPublishOnArtifactoryJob = BackendBuildAndPublishOnArtifactoryJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(backendBuildAndPublishOnArtifactoryJob);

    const releaseCommitAndPrepareNextVersionJob = ReleaseCommitAndPrepareNextVersionJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(releaseCommitAndPrepareNextVersionJob);

    return new Workflow(ReleaseWorkflow.workflowName, [
      // PREPARE
      new workflow.WorkflowJob(setupJob, { context: config.jobContext, name: 'Setup' }),
      new workflow.WorkflowJob(slackAnnouncementJob, {
        context: config.jobContext,
        name: 'Announce release is starting',
        message: `🚀 Starting APIM ${environment.graviteeioVersion} release!`,
      }),

      // APIM Portal
      new workflow.WorkflowJob(webuiBuildJob, {
        context: config.jobContext,
        name: 'Build APIM Portal and publish image',
        'apim-ui-project': 'gravitee-apim-portal-webui',
        'docker-image-name': 'apim-portal-ui',
        requires: ['Setup'],
      }),
      new workflow.WorkflowJob(webuiPublishArtifactoryJob, {
        context: config.jobContext,
        name: 'Publish APIM Portal to artifactory',
        'apim-ui-project': 'gravitee-apim-portal-webui',
        requires: ['Build APIM Portal and publish image'],
      }),

      // APIM Console
      new workflow.WorkflowJob(webuiBuildJob, {
        context: config.jobContext,
        name: 'Build APIM Console and publish image',
        'apim-ui-project': 'gravitee-apim-console-webui',
        'docker-image-name': 'apim-management-ui',
        requires: ['Setup'],
      }),
      new workflow.WorkflowJob(webuiPublishArtifactoryJob, {
        context: config.jobContext,
        name: 'Publish APIM Console to artifactory',
        'apim-ui-project': 'gravitee-apim-console-webui',
        requires: ['Build APIM Console and publish image'],
      }),

      // APIM Backend
      new workflow.WorkflowJob(backendBuildAndPublishOnArtifactoryJob, {
        context: config.jobContext,
        name: 'Backend build and publish to artifactory',
        requires: ['Setup'],
      }),

      // Commit and set next version
      new workflow.WorkflowJob(releaseCommitAndPrepareNextVersionJob, {
        context: config.jobContext,
        name: 'Commit and prepare next version',
        requires: ['Backend build and publish to artifactory', 'Publish APIM Console to artifactory', 'Publish APIM Portal to artifactory'],
      }),
    ]);
  }
}
