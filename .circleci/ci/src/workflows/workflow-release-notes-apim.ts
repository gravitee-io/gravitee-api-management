import { Config, workflow, Workflow } from '@circleci/circleci-config-sdk';
import { CircleCIEnvironment } from '../pipelines';
import { ReleaseNotesApimJob } from '../jobs';
import { config } from '../config';

export class ReleaseNotesApimWorkflow {
  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const releaseNotesApimJob = ReleaseNotesApimJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(releaseNotesApimJob);

    return new Workflow('release-notes-apim', [
      new workflow.WorkflowJob(releaseNotesApimJob, {
        context: config.jobContext,
        name: 'Generate release note and create PR',
      }),
    ]);
  }
}
