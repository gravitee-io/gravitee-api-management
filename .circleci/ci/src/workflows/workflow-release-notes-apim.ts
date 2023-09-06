import { Config, Workflow } from '@circleci/circleci-config-sdk';
import { CircleCIEnvironment } from '../pipelines';
import { ReleaseNotesApimJob } from '../jobs';

export class ReleaseNotesApimWorkflow {
  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const releaseNotesApimJob = ReleaseNotesApimJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(releaseNotesApimJob);

    return new Workflow('release-notes-apim', [releaseNotesApimJob]);
  }
}
