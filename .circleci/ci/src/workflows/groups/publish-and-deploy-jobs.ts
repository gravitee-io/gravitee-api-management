/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Config, workflow } from '../../circleci-config';
import {
  AikidoScanDockerImagesJob,
  DeployOnAzureJob,
  DeployOnNextGenIntegrationJob,
  PublishJob,
  ReleaseHelmJob,
  TriggerSaasDockerImagesJob,
} from '../../jobs';
import { CircleCIEnvironment } from '../../pipelines';
import { isMasterBranch } from '../../utils';
import { config } from '../../config';

/**
 * What a push to a default or release branch does on top of refreshing the environment:
 * publish the snapshots, hand the images to the SaaS pipeline, and roll the deployments.
 *
 * The publications used to wait on the six per-module test jobs. Those jobs no longer run on
 * this path — a pull request has already run them, on the scope its changes could affect — so
 * they wait on the build instead. The trade is deliberate: what reaches the environment and the
 * snapshot repositories has been tested before merge, not after it.
 */
export function publishAndDeployJobs(dynamicConfig: Config, environment: CircleCIEnvironment): workflow.WorkflowJob[] {
  const publishOnArtifactoryJob = PublishJob.create(dynamicConfig, environment, 'artifactory');
  dynamicConfig.addJob(publishOnArtifactoryJob);

  const publishOnNexusJob = PublishJob.create(dynamicConfig, environment, 'nexus');
  dynamicConfig.addJob(publishOnNexusJob);

  const releaseHelmDryRunJob = ReleaseHelmJob.create(dynamicConfig, environment);
  dynamicConfig.addJob(releaseHelmDryRunJob);

  const deployOnAzureJob = DeployOnAzureJob.create(dynamicConfig, environment);
  dynamicConfig.addJob(deployOnAzureJob);

  const deployOnNextGenIntegrationJob = DeployOnNextGenIntegrationJob.create(dynamicConfig, environment);
  if (isMasterBranch(environment.branch)) {
    dynamicConfig.addJob(deployOnNextGenIntegrationJob);
  }

  const runTriggerSaasDockerImagesJob = TriggerSaasDockerImagesJob.create({ ...environment, isDryRun: false }, 'dev');
  dynamicConfig.addJob(runTriggerSaasDockerImagesJob);

  const productImages = [
    'Build APIM Management API docker image',
    'Build APIM Gateway docker image',
    'Build APIM Console docker image',
    'Build APIM Portal docker image',
  ];

  return [
    new workflow.WorkflowJob(runTriggerSaasDockerImagesJob, {
      context: [...config.jobContext, 'keeper-orb-publishing'],
      name: 'Trigger SaaS Docker images creation',
      requires: productImages,
    }),
    new workflow.WorkflowJob(releaseHelmDryRunJob, {
      name: 'Publish Helm chart (internal repo)',
      context: config.jobContext,
      requires: ['Trigger SaaS Docker images creation'],
    }),
    new workflow.WorkflowJob(publishOnArtifactoryJob, {
      name: 'Publish on artifactory',
      context: config.jobContext,
      requires: ['Build backend'],
    }),
    new workflow.WorkflowJob(publishOnNexusJob, {
      name: 'Publish on nexus',
      context: config.jobContext,
      requires: ['Build backend'],
    }),
    new workflow.WorkflowJob(deployOnAzureJob, {
      name: 'Deploy on Azure cluster',
      context: config.jobContext,
      requires: productImages,
    }),
    ...(isMasterBranch(environment.branch)
      ? [
          new workflow.WorkflowJob(deployOnNextGenIntegrationJob, {
            name: 'Deploy on NextGen Integration environment',
            context: config.jobContext,
            requires: ['Trigger SaaS Docker images creation', 'Publish Helm chart (internal repo)'],
          }),
        ]
      : []),

    // Aikido scans the images this path actually pushes. The chainguard variants are built by
    // their own manually triggered workflows, so asking for them here would leave the scan
    // waiting on jobs that are not in this workflow.
    ...AikidoScanDockerImagesJob.workflowJobs(dynamicConfig, environment, false, '', false),
  ];
}
