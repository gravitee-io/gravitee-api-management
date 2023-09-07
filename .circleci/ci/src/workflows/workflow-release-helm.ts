import { Config, workflow, Workflow } from '@circleci/circleci-config-sdk';
import { CircleCIEnvironment } from '../pipelines';
import { config } from '../config';
import { ReleaseHelmJob, TestApimChartsJob } from '../jobs/helm';

export class ReleaseHelmWorkflow {
  private static workflowName = 'release_helm';

  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const testApimChartsJob = TestApimChartsJob.create(dynamicConfig);
    dynamicConfig.addJob(testApimChartsJob);

    const releaseHelmJob = ReleaseHelmJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(releaseHelmJob);

    return new Workflow(ReleaseHelmWorkflow.workflowName, [
      new workflow.WorkflowJob(testApimChartsJob, { name: 'APIM - Lint & Test' }),
      new workflow.WorkflowJob(releaseHelmJob, { requires: ['APIM - Lint & Test'], context: config.jobContext }),
    ]);
  }
}
