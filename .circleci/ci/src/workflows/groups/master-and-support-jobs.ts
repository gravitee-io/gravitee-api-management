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
  BuildDockerChainguardImageJob,
  CommunityBuildBackendJob,
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
 * Everything a push to master or to a support branch adds on top of the common jobs:
 * the chainguard images, the community build check, the SaaS trigger, the Helm and Maven
 * publications, and the two environment deployments.
 *
 * Heterogeneous on purpose for now — this is a verbatim move. Splitting it into the groups
 * that actually belong together is a change of behaviour, not a refactor, and comes later.
 */
export function masterAndSupportJobs(dynamicConfig: Config, environment: CircleCIEnvironment): workflow.WorkflowJob[] {
  const communityBuildJob = CommunityBuildBackendJob.create(dynamicConfig, environment);
  dynamicConfig.addJob(communityBuildJob);

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

  const runTriggerSaasDockerImagesJob = TriggerSaasDockerImagesJob.create(
    {
      ...environment,
      isDryRun: false,
    },
    'dev',
  );
  dynamicConfig.addJob(runTriggerSaasDockerImagesJob);

  const buildDockerChainguardImageJob = BuildDockerChainguardImageJob.create(dynamicConfig, environment, false);
  dynamicConfig.addJob(buildDockerChainguardImageJob);

  return [
    new workflow.WorkflowJob(buildDockerChainguardImageJob, {
      context: config.jobContext,
      name: `Build APIM Management API chainguard docker image`,
      requires: ['Build backend'],
      'apim-project': config.components.managementApi.project,
      'apim-project-workdir': config.components.managementApi.distribution,
      'docker-context': 'target',
      'docker-image-name': config.components.managementApi.image,
    }),
    new workflow.WorkflowJob(buildDockerChainguardImageJob, {
      context: config.jobContext,
      name: `Build APIM Gateway chainguard docker image`,
      requires: ['Build backend'],
      'apim-project': config.components.gateway.project,
      'apim-project-workdir': config.components.gateway.distribution,
      'docker-context': 'target',
      'docker-image-name': config.components.gateway.image,
    }),
    new workflow.WorkflowJob(buildDockerChainguardImageJob, {
      context: config.jobContext,
      name: `Build APIM Console chainguard docker image`,
      requires: ['Build APIM Console'],
      'apim-project': config.components.console.project,
      'apim-project-workdir': config.components.console.workdir,
      'docker-context': '.',
      'docker-image-name': config.components.console.image,
    }),
    new workflow.WorkflowJob(buildDockerChainguardImageJob, {
      context: config.jobContext,
      name: `Build APIM Portal chainguard docker image`,
      requires: ['Build APIM Portal'],
      'apim-project': config.components.portal.project,
      'apim-project-workdir': config.components.portal.workdir,
      'docker-context': '.',
      'docker-image-name': config.components.portal.image,
    }),
    new workflow.WorkflowJob(buildDockerChainguardImageJob, {
      context: config.jobContext,
      name: `Build Gamma Console chainguard docker image`,
      requires: ['Build Gamma Console'],
      'apim-project': config.components.gamma.project,
      'apim-project-workdir': config.components.gamma.workdir,
      'docker-context': '.',
      'docker-image-name': config.components.gamma.image,
    }),
    new workflow.WorkflowJob(communityBuildJob, {
      name: 'Check build as Community user',
      context: config.jobContext,
    }),
    // Trigger SaaS Docker images creation
    new workflow.WorkflowJob(runTriggerSaasDockerImagesJob, {
      context: [...config.jobContext, 'keeper-orb-publishing'],
      name: 'Trigger SaaS Docker images creation',
      requires: [
        'Build APIM Management API docker image',
        'Build APIM Gateway docker image',
        'Build APIM Console docker image',
        'Build APIM Portal docker image',
      ],
    }),
    new workflow.WorkflowJob(releaseHelmDryRunJob, {
      name: 'Publish Helm chart (internal repo)',
      context: config.jobContext,
      requires: ['Trigger SaaS Docker images creation'],
    }),
    new workflow.WorkflowJob(publishOnArtifactoryJob, {
      name: 'Publish on artifactory',
      context: config.jobContext,
      requires: ['Test definition', 'Test gateway', 'Test plugins', 'Test reporters', 'Test repository', 'Test rest-api'],
    }),
    new workflow.WorkflowJob(publishOnNexusJob, {
      name: 'Publish on nexus',
      context: config.jobContext,
      requires: ['Test definition', 'Test gateway', 'Test plugins', 'Test reporters', 'Test repository', 'Test rest-api'],
    }),
    new workflow.WorkflowJob(deployOnAzureJob, {
      name: 'Deploy on Azure cluster',
      context: config.jobContext,
      requires: [
        'Test definition',
        'Test gateway',
        'Test plugins',
        'Test reporters',
        'Test repository',
        'Test rest-api',
        'Build APIM Management API docker image',
        'Build APIM Gateway docker image',
        'Build APIM Console docker image',
        'Build APIM Portal docker image',
      ],
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

    // Aikido image scans, once every variant of a component has been pushed
    ...AikidoScanDockerImagesJob.workflowJobs(dynamicConfig, environment, false, '', true),
  ];
}
