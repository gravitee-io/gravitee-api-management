import { Config, workflow, Workflow } from '@circleci/circleci-config-sdk';
import { PackageBundleJob } from '../jobs';

export class PackageBundleWorkflow {
  static create(dynamicConfig: Config, graviteeioVersion: string, isDryRun: boolean) {
    const bundleJob = PackageBundleJob.create(dynamicConfig, graviteeioVersion, isDryRun);
    dynamicConfig.addJob(bundleJob);
    return new Workflow('package_bundle', [new workflow.WorkflowJob(bundleJob, { context: ['cicd-orchestrator'] })]);
  }
}
