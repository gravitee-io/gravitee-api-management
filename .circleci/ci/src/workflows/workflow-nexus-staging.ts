import { SlackAnnouncementJob, SetupJob } from '../jobs';
import { Config, Workflow, workflow } from '@circleci/circleci-config-sdk';
import { config } from '../config';
import { CircleCIEnvironment } from '../pipelines';
import { NexusStagingJob } from '../jobs/backend';

export class WorkflowNexusStaging {
  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const setupJob = SetupJob.create(dynamicConfig);
    dynamicConfig.addJob(setupJob);

    const nexusStagingJob = NexusStagingJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(nexusStagingJob);

    const slackAnnouncementJob = SlackAnnouncementJob.create(dynamicConfig);
    dynamicConfig.addJob(slackAnnouncementJob);

    const jobs = [
      new workflow.WorkflowJob(setupJob, { context: config.jobContext, name: 'Setup' }),
      new workflow.WorkflowJob(nexusStagingJob, { context: config.jobContext, name: 'Nexus staging', requires: ['Setup'] }),
      new workflow.WorkflowJob(slackAnnouncementJob, {
        context: config.jobContext,
        name: 'Announce end of release',
        requires: ['Nexus staging'],
        message: `🎆 APIM - ${environment.graviteeioVersion} released!`,
      }),
    ];

    return new Workflow('nexus_staging', jobs);
  }
}
