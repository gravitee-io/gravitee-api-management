import { Config, workflow, Workflow } from '@circleci/circleci-config-sdk';
import { BuildBackendJob, SetupJob, ValidateJob } from '../jobs';
import { E2EGenerateSDKJob, E2ELintBuildJob, E2ETestJob } from '../jobs/e2e';
import { config } from '../config';
import { CircleCIEnvironment } from '../pipelines';

export class BridgeCompatibilityTestsWorkflow {
  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const setupJob = SetupJob.create(dynamicConfig);
    dynamicConfig.addJob(setupJob);

    const validateJob = ValidateJob.create(dynamicConfig);
    dynamicConfig.addJob(validateJob);

    const buildBackendJob = BuildBackendJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(buildBackendJob);

    const e2eGenerateSdkJob = E2EGenerateSDKJob.create(dynamicConfig);
    dynamicConfig.addJob(e2eGenerateSdkJob);

    const e2eLintBuildJob = E2ELintBuildJob.create(dynamicConfig);
    dynamicConfig.addJob(e2eLintBuildJob);

    const e2eTestJob = E2ETestJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(e2eTestJob);

    const jobs = [
      new workflow.WorkflowJob(setupJob, { context: config.jobContext, name: 'Setup' }),
      new workflow.WorkflowJob(validateJob, { context: config.jobContext, name: 'Validate', requires: ['Setup'] }),
      new workflow.WorkflowJob(buildBackendJob, { context: config.jobContext, name: 'Build backend', requires: ['Validate'] }),
      new workflow.WorkflowJob(e2eGenerateSdkJob, {
        context: config.jobContext,
        name: 'Generate e2e tests SDK',
        requires: ['Build backend'],
      }),
      new workflow.WorkflowJob(e2eLintBuildJob, {
        context: config.jobContext,
        name: 'Lint & Build APIM e2e',
        requires: ['Generate e2e tests SDK'],
      }),
      new workflow.WorkflowJob(e2eTestJob, {
        context: config.jobContext,
        name: 'E2E - << matrix.execution_mode >> - << matrix.apim_client_tag >>',
        requires: ['Lint & Build APIM e2e'],
        matrix: {
          execution_mode: ['v3', 'v4-emulation-engine'],
          database: ['bridge'],
          apim_client_tag: ['4.0.x-latest', '3.20.x-latest', '3.19.x-latest'],
        },
      }),
    ];

    return new Workflow('bridge_compatibility_tests', jobs);
  }
}
