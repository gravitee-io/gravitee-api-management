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
import { Config, workflow, Workflow } from '../circleci-config';
import {
  AikidoScanDockerImagesJob,
  BackendBuildAndPublishOnDownloadWebsiteJob,
  BuildDockerBackendImageJob,
  BuildDockerChainguardImageJob,
  BuildDockerChainguardFipsImageJob,
  BuildDockerWebUiImageJob,
  ConsoleWebuiBuildJob,
  GammaWebuiBuildJob,
  PackageBundleJob,
  PortalWebuiBuildJob,
  PublishRpmPackagesJob,
  ReleaseHelmJob,
  ReleaseNotesApimJob,
  SetupJob,
  SlackAnnouncementJob,
  TriggerApimApiDocsPipelineJob,
  TriggerSaasDockerImagesJob,
} from '../jobs';
import { CircleCIEnvironment } from '../pipelines';
import { DISTRIBUTION_TAG_FILTER } from '../utils';
import { config } from '../config';

export class DistributionReleaseWorkflow {
  private static workflowName = 'distribution_release';

  /**
   * Repeated on every job below. The continued configuration is a pipeline of its own and inherits
   * nothing from the filter that let the setup job run, so a job without it simply does not run.
   */
  private static readonly tagOnly = {
    branches: { ignore: ['/.*/'] },
    tags: { only: [DISTRIBUTION_TAG_FILTER] },
  };

  static create(dynamicConfig: Config, environment: CircleCIEnvironment) {
    const setupJob = SetupJob.create(dynamicConfig);
    dynamicConfig.addJob(setupJob);

    const slackAnnouncementJob = SlackAnnouncementJob.create(dynamicConfig);
    dynamicConfig.addJob(slackAnnouncementJob);

    const consoleWebuiBuildJob = ConsoleWebuiBuildJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(consoleWebuiBuildJob);

    const portalWebuiBuildJob = PortalWebuiBuildJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(portalWebuiBuildJob);

    const gammaWebuiBuildJob = GammaWebuiBuildJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(gammaWebuiBuildJob);

    const buildDockerWebUiImageJob = BuildDockerWebUiImageJob.create(dynamicConfig, environment, true);
    dynamicConfig.addJob(buildDockerWebUiImageJob);
    const buildDockerBackendImageJob = BuildDockerBackendImageJob.create(dynamicConfig, environment, true);
    dynamicConfig.addJob(buildDockerBackendImageJob);

    // Chainguard component images: published to Docker Hub (<version>-chainguard) like the
    // alpine/debian variants; the private azurecr base is pulled via a second login.
    const buildDockerChainguardImageJob = BuildDockerChainguardImageJob.create(dynamicConfig, environment, true);
    dynamicConfig.addJob(buildDockerChainguardImageJob);
    // FIPS component images: all components (gateway, management-api, portal, console, gamma),
    // published to the private Azure registry only (<version>-chainguard-fips), never Docker Hub.
    const buildDockerChainguardFipsImageJob = BuildDockerChainguardFipsImageJob.create(dynamicConfig, environment, true);
    dynamicConfig.addJob(buildDockerChainguardFipsImageJob);

    const backendBuildAndPublishOnDownloadWebsiteJob = BackendBuildAndPublishOnDownloadWebsiteJob.create(dynamicConfig, environment, true);
    dynamicConfig.addJob(backendBuildAndPublishOnDownloadWebsiteJob);

    const packageBundleJob = PackageBundleJob.create(dynamicConfig, environment.graviteeioVersion, environment.isDryRun);
    dynamicConfig.addJob(packageBundleJob);

    const releaseHelmJob = ReleaseHelmJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(releaseHelmJob);

    const publishRpmPackagesJob = PublishRpmPackagesJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(publishRpmPackagesJob);

    const releaseNoteApimJob = ReleaseNotesApimJob.create(dynamicConfig, environment);
    dynamicConfig.addJob(releaseNoteApimJob);

    const runTriggerSaasDockerImagesJob = TriggerSaasDockerImagesJob.create(environment, 'prod');
    dynamicConfig.addJob(runTriggerSaasDockerImagesJob);

    const runTriggerSaasChainguardDockerImagesJob = TriggerSaasDockerImagesJob.create(environment, 'prod', 'chainguard');
    dynamicConfig.addJob(runTriggerSaasChainguardDockerImagesJob);

    const runTriggerSaasChainguardFipsDockerImagesJob = TriggerSaasDockerImagesJob.create(environment, 'prod', 'chainguard-fips');
    dynamicConfig.addJob(runTriggerSaasChainguardFipsDockerImagesJob);

    const triggerApimApiDocsPipelineJob = TriggerApimApiDocsPipelineJob.create(environment);
    dynamicConfig.addJob(triggerApimApiDocsPipelineJob);

    const jobs = [
      // PREPARE
      new workflow.WorkflowJob(setupJob, { context: config.jobContext, name: 'Setup' }),
      new workflow.WorkflowJob(slackAnnouncementJob, {
        context: config.jobContext,
        name: 'Announce release is starting',
        message: `🚀 Starting APIM ${environment.graviteeioVersion} release!`,
      }),

      // APIM Portal
      new workflow.WorkflowJob(portalWebuiBuildJob, {
        context: config.jobContext,
        name: 'Build APIM Portal',
        requires: ['Setup'],
      }),
      new workflow.WorkflowJob(buildDockerWebUiImageJob, {
        context: config.jobContext,
        name: `Build APIM Portal docker image for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
        requires: ['Build APIM Portal'],
        'apim-project': config.components.portal.project,
        'apim-project-workdir': config.components.portal.workdir,
        'docker-context': '.',
        'docker-image-name': config.components.portal.image,
      }),

      // APIM Console
      new workflow.WorkflowJob(consoleWebuiBuildJob, {
        context: config.jobContext,
        name: 'Build APIM Console',
        requires: ['Setup'],
      }),
      new workflow.WorkflowJob(buildDockerWebUiImageJob, {
        context: config.jobContext,
        name: `Build APIM Console docker image for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
        requires: ['Build APIM Console'],
        'apim-project': config.components.console.project,
        'apim-project-workdir': config.components.console.workdir,
        'docker-context': '.',
        'docker-image-name': config.components.console.image,
      }),

      // Gamma Console
      new workflow.WorkflowJob(gammaWebuiBuildJob, {
        context: config.jobContext,
        name: 'Build Gamma Console',
        requires: ['Setup'],
      }),
      new workflow.WorkflowJob(buildDockerWebUiImageJob, {
        context: config.jobContext,
        name: `Build Gamma Console docker image for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
        requires: ['Build Gamma Console'],
        'apim-project': config.components.gamma.project,
        'apim-project-workdir': config.components.gamma.workdir,
        'docker-context': '.',
        'docker-image-name': config.components.gamma.image,
      }),

      // APIM Backend
      new workflow.WorkflowJob(backendBuildAndPublishOnDownloadWebsiteJob, {
        context: config.jobContext,
        name: 'Backend build and publish on download website',
        requires: ['Setup'],
      }),
      new workflow.WorkflowJob(buildDockerBackendImageJob, {
        context: config.jobContext,
        name: `Build APIM Management API docker image for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
        requires: ['Backend build and publish on download website'],
        'apim-project': config.components.managementApi.project,
        'apim-project-workdir': config.components.managementApi.distribution,
        'docker-context': 'target',
        'docker-image-name': config.components.managementApi.image,
      }),
      new workflow.WorkflowJob(buildDockerBackendImageJob, {
        context: config.jobContext,
        name: `Build APIM Gateway docker image for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
        requires: ['Backend build and publish on download website'],
        'apim-project': config.components.gateway.project,
        'apim-project-workdir': config.components.gateway.distribution,
        'docker-context': 'target',
        'docker-image-name': config.components.gateway.image,
      }),

      // Chainguard component images (Docker Hub, <version>-chainguard)
      new workflow.WorkflowJob(buildDockerChainguardImageJob, {
        context: config.jobContext,
        name: `Build APIM Portal chainguard docker image for APIM ${environment.graviteeioVersion}`,
        requires: ['Build APIM Portal'],
        'apim-project': config.components.portal.project,
        'apim-project-workdir': config.components.portal.workdir,
        'docker-context': '.',
        'docker-image-name': config.components.portal.image,
      }),
      new workflow.WorkflowJob(buildDockerChainguardImageJob, {
        context: config.jobContext,
        name: `Build APIM Console chainguard docker image for APIM ${environment.graviteeioVersion}`,
        requires: ['Build APIM Console'],
        'apim-project': config.components.console.project,
        'apim-project-workdir': config.components.console.workdir,
        'docker-context': '.',
        'docker-image-name': config.components.console.image,
      }),
      new workflow.WorkflowJob(buildDockerChainguardImageJob, {
        context: config.jobContext,
        name: `Build Gamma Console chainguard docker image for APIM ${environment.graviteeioVersion}`,
        requires: ['Build Gamma Console'],
        'apim-project': config.components.gamma.project,
        'apim-project-workdir': config.components.gamma.workdir,
        'docker-context': '.',
        'docker-image-name': config.components.gamma.image,
      }),
      new workflow.WorkflowJob(buildDockerChainguardImageJob, {
        context: config.jobContext,
        name: `Build APIM Management API chainguard docker image for APIM ${environment.graviteeioVersion}`,
        requires: ['Backend build and publish on download website'],
        'apim-project': config.components.managementApi.project,
        'apim-project-workdir': config.components.managementApi.distribution,
        'docker-context': 'target',
        'docker-image-name': config.components.managementApi.image,
      }),
      new workflow.WorkflowJob(buildDockerChainguardImageJob, {
        context: config.jobContext,
        name: `Build APIM Gateway chainguard docker image for APIM ${environment.graviteeioVersion}`,
        requires: ['Backend build and publish on download website'],
        'apim-project': config.components.gateway.project,
        'apim-project-workdir': config.components.gateway.distribution,
        'docker-context': 'target',
        'docker-image-name': config.components.gateway.image,
      }),

      // Chainguard FIPS component images (Azure registry only, <version>-chainguard-fips).
      // Java components use the java-fips base; the UIs use the nginx-fips base.
      new workflow.WorkflowJob(buildDockerChainguardFipsImageJob, {
        context: config.jobContext,
        name: `Build APIM Management API chainguard-fips docker image for APIM ${environment.graviteeioVersion}`,
        requires: ['Backend build and publish on download website'],
        'apim-project': config.components.managementApi.project,
        'apim-project-workdir': config.components.managementApi.distribution,
        'docker-context': 'target',
        'docker-image-name': config.components.managementApi.image,
        'docker-fips-base-image': config.docker.fipsJavaBaseImage,
      }),
      new workflow.WorkflowJob(buildDockerChainguardFipsImageJob, {
        context: config.jobContext,
        name: `Build APIM Gateway chainguard-fips docker image for APIM ${environment.graviteeioVersion}`,
        requires: ['Backend build and publish on download website'],
        'apim-project': config.components.gateway.project,
        'apim-project-workdir': config.components.gateway.distribution,
        'docker-context': 'target',
        'docker-image-name': config.components.gateway.image,
        'docker-fips-base-image': config.docker.fipsJavaBaseImage,
      }),
      new workflow.WorkflowJob(buildDockerChainguardFipsImageJob, {
        context: config.jobContext,
        name: `Build APIM Portal chainguard-fips docker image for APIM ${environment.graviteeioVersion}`,
        requires: ['Build APIM Portal'],
        'apim-project': config.components.portal.project,
        'apim-project-workdir': config.components.portal.workdir,
        'docker-context': '.',
        'docker-image-name': config.components.portal.image,
        'docker-fips-base-image': config.docker.fipsNginxBaseImage,
      }),
      new workflow.WorkflowJob(buildDockerChainguardFipsImageJob, {
        context: config.jobContext,
        name: `Build APIM Console chainguard-fips docker image for APIM ${environment.graviteeioVersion}`,
        requires: ['Build APIM Console'],
        'apim-project': config.components.console.project,
        'apim-project-workdir': config.components.console.workdir,
        'docker-context': '.',
        'docker-image-name': config.components.console.image,
        'docker-fips-base-image': config.docker.fipsNginxBaseImage,
      }),
      new workflow.WorkflowJob(buildDockerChainguardFipsImageJob, {
        context: config.jobContext,
        name: `Build Gamma Console chainguard-fips docker image for APIM ${environment.graviteeioVersion}`,
        requires: ['Build Gamma Console'],
        'apim-project': config.components.gamma.project,
        'apim-project-workdir': config.components.gamma.workdir,
        'docker-context': '.',
        'docker-image-name': config.components.gamma.image,
        'docker-fips-base-image': config.docker.fipsNginxBaseImage,
      }),

      // Package bundle
      new workflow.WorkflowJob(packageBundleJob, {
        context: config.jobContext,
        name: 'Package bundle',
        // What the commit job used to wait for. The commit itself happened before the tag that
        // started this pipeline.
        requires: ['Backend build and publish on download website', 'Build APIM Console', 'Build APIM Portal', 'Build Gamma Console'],
      }),

      // Publish RPM Packages
      new workflow.WorkflowJob(publishRpmPackagesJob, {
        context: config.jobContext,
        name: `Build and push RPM packages for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
        requires: ['Package bundle'],
      }),

      // Trigger SaaS Docker images creation
      new workflow.WorkflowJob(runTriggerSaasDockerImagesJob, {
        context: [...config.jobContext, 'keeper-orb-publishing'],
        name: 'Trigger SaaS Docker images creation',
        requires: [
          `Build APIM Portal docker image for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
          `Build APIM Console docker image for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
          `Build APIM Management API docker image for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
          `Build APIM Gateway docker image for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
        ],
      }),

      // Trigger SaaS Chainguard Docker images creation (built from the chainguard component images)
      new workflow.WorkflowJob(runTriggerSaasChainguardDockerImagesJob, {
        context: [...config.jobContext, 'keeper-orb-publishing'],
        name: 'Trigger SaaS Chainguard Docker images creation',
        requires: [
          `Build APIM Portal chainguard docker image for APIM ${environment.graviteeioVersion}`,
          `Build APIM Console chainguard docker image for APIM ${environment.graviteeioVersion}`,
          `Build Gamma Console chainguard docker image for APIM ${environment.graviteeioVersion}`,
          `Build APIM Management API chainguard docker image for APIM ${environment.graviteeioVersion}`,
          `Build APIM Gateway chainguard docker image for APIM ${environment.graviteeioVersion}`,
        ],
      }),

      // Trigger SaaS Chainguard FIPS Docker images creation
      new workflow.WorkflowJob(runTriggerSaasChainguardFipsDockerImagesJob, {
        context: [...config.jobContext, 'keeper-orb-publishing'],
        name: 'Trigger SaaS Chainguard FIPS Docker images creation',
        requires: [
          `Build APIM Management API chainguard-fips docker image for APIM ${environment.graviteeioVersion}`,
          `Build APIM Gateway chainguard-fips docker image for APIM ${environment.graviteeioVersion}`,
          `Build APIM Portal chainguard-fips docker image for APIM ${environment.graviteeioVersion}`,
          `Build APIM Console chainguard-fips docker image for APIM ${environment.graviteeioVersion}`,
          `Build Gamma Console chainguard-fips docker image for APIM ${environment.graviteeioVersion}`,
        ],
      }),

      // Trigger gravitee-apim-api-docs ingestion (fire-and-forget; the docs
      // pipeline absorbs the Sonatype → Maven Central propagation delay).
      new workflow.WorkflowJob(triggerApimApiDocsPipelineJob, {
        context: [...config.jobContext, 'keeper-orb-publishing'],
        name: 'Trigger APIM API docs ingestion',
        // Not the core's publication, which is another lane's now: the docs pipeline polls Maven
        // Central itself, for up to an hour, so it only has to be told once this release is real.
        requires: ['Trigger SaaS Docker images creation'],
      }),

      // Release Helm chart
      new workflow.WorkflowJob(releaseHelmJob, {
        context: config.jobContext,
        name: 'Release Helm Chart',
        requires: ['Trigger SaaS Docker images creation'],
      }),

      // Create Release note pull request
      new workflow.WorkflowJob(releaseNoteApimJob, {
        context: config.jobContext,
        name: 'Create release note pull request',
        requires: [
          'Release Helm Chart',
          `Build and push RPM packages for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
        ],
      }),

      // Notify APIM team
      new workflow.WorkflowJob(slackAnnouncementJob, {
        context: config.jobContext,
        name: 'Announce release is completed',
        message: `🎆 APIM - ${environment.graviteeioVersion} released!`,
        requires: [
          'Release Helm Chart',
          'Trigger APIM API docs ingestion',
          `Build and push RPM packages for APIM ${environment.graviteeioVersion}${environment.isDryRun ? ' - Dry Run' : ''}`,
        ],
      }),

      // Aikido image scans, once every variant of a component has been pushed
      ...AikidoScanDockerImagesJob.workflowJobs(dynamicConfig, environment, true, ` for APIM ${environment.graviteeioVersion}`, [
        'chainguard',
        'chainguard-fips',
      ]),
    ];

    return new Workflow(
      DistributionReleaseWorkflow.workflowName,
      jobs.map((job) => job.withFilters(DistributionReleaseWorkflow.tagOnly)),
    );
  }
}
