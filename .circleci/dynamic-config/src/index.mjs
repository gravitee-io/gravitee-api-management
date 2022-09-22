import circle from '@circleci/circleci-config-sdk';
import { createInstallJdkCommand } from './config/commands/install-jdk.mjs';
import { createPrepareEnvVarsCommand } from './config/commands/prepare-env-vars.mjs';
import { createRestoreMavenCacheCommand } from './config/commands/restore-maven-job-cache.mjs';

import { NodeLTS } from './config/executors/node-lts.mjs';
import { OpenJDK } from './config/executors/openjdk.mjs';
import { Ubuntu } from './config/executors/ubuntu.mjs';
import { createCommunityBuildJob } from './config/jobs/community-build.mjs';
import { createSetupJob } from './config/jobs/setup.mjs';
import { DockerExecutor, ReusedExecutor, WorkflowJob } from './sdk/index.mjs';

import { artifactoryOrb } from './config/orbs/artifactory.mjs';
import { awsCliOrb } from './config/orbs/aws-cli.mjs';
import { awsS3Orb } from './config/orbs/aws-s3.mjs';
import { ghOrb } from './config/orbs/gh.mjs';
import { keeperOrb } from './config/orbs/keeper.mjs';
import { slackOrb } from './config/orbs/slack.mjs';
import { createSaveMavenCacheCommand } from './config/commands/save-maven-job-cache.mjs';
import { createPullRequestWorkflow } from './config/workflows/pull-request.mjs';
import { createNotifyOnFailureCommand } from './config/commands/notify-on-failure.mjs';
import { createValidateJob } from './config/jobs/validate.mjs';
import { createBuildJob } from './config/jobs/build.mjs';
import { createGetApimVersionCommand } from './config/commands/get-version.mjs';
import { createBuildBackendImagesCommand } from './config/commands/build-backend-images.mjs';
import { createBuildImagesJob } from './config/jobs/build-images.mjs';
import { createSaveBackendImagesCommand } from './config/commands/save-backend-images.mjs';
import { createTestJob } from './config/jobs/test.mjs';
import { createSonarCloudAnalysisJob } from './config/jobs/sonarcloud-analysis.mjs';
import { createRunSonarCommand } from './config/commands/run-sonar.mjs';

const outputFile = path.join(__dirname, '..', '..', 'dynamic-config.yml');

const setUpJob = createSetupJob(new ReusedExecutor(Ubuntu, { class: 'small' }));
const communityBuildJob = createCommunityBuildJob(new ReusedExecutor(OpenJDK), { version: '11' });
const validateJob = createValidateJob(new ReusedExecutor(OpenJDK), { class: 'small' });
const buildJob = createBuildJob(new ReusedExecutor(OpenJDK, { class: 'large' }));
const buildImagesJob = createBuildImagesJob(new ReusedExecutor(OpenJDK));
const testJob = createTestJob(new ReusedExecutor(OpenJDK, { class: 'medium+' }), 'test', 'main-modules');
const testPluginJob = createTestJob(new ReusedExecutor(OpenJDK, { class: 'medium' }), 'test-plugin', 'plugin-modules');
const testRepositoryJob = createTestJob(
  new ReusedExecutor(OpenJDK, { class: 'medium' }),
  'test-repository',
  'repository-modules',
);

const sonarCloudAnalysisJob = createSonarCloudAnalysisJob(new DockerExecutor('sonarsource/sonar-scanner-cli'));
const context = 'cicd-orchestrator';

const config = new circle.Config();

config
  .importOrb(slackOrb)
  .importOrb(keeperOrb)
  .importOrb(artifactoryOrb)
  .importOrb(awsCliOrb)
  .importOrb(awsS3Orb)
  .importOrb(ghOrb)
  .defineParameter('gio_action', 'enum', 'pull_requests', 'The workflow to trigger', [
    'release',
    'package_bundle',
    'nexus_staging',
    'pull_requests',
    'build_rpm_&_docker_images',
    'changelog_apim',
  ])
  .addReusableExecutor(Ubuntu)
  .addReusableExecutor(OpenJDK)
  .addReusableExecutor(NodeLTS)
  .addReusableCommand(createInstallJdkCommand(17))
  .addReusableCommand(createRestoreMavenCacheCommand())
  .addReusableCommand(createSaveMavenCacheCommand())
  .addReusableCommand(createNotifyOnFailureCommand('C02JENTV2AX'))
  .addReusableCommand(createGetApimVersionCommand())
  .addReusableCommand(createBuildBackendImagesCommand())
  .addReusableCommand(createSaveBackendImagesCommand())
  .addReusableCommand(createRunSonarCommand())
  .addReusableCommand(
    createPrepareEnvVarsCommand({
      BUILD_ID: '${CIRCLE_BUILD_NUM}',
      BUILD_NUMBER: '${CIRCLE_BUILD_NUM}',
      GIT_COMMIT: '$(git rev-parse --short HEAD)',
    }),
  )
  .addJob(setUpJob)
  .addJob(validateJob)
  .addJob(communityBuildJob)
  .addJob(buildJob)
  .addJob(buildImagesJob)
  .addJob(testJob)
  .addJob(testPluginJob)
  .addJob(testRepositoryJob)
  .addJob(sonarCloudAnalysisJob)
  .addWorkflow(
    createPullRequestWorkflow([
      new WorkflowJob(communityBuildJob, {
        context,
        filters: {
          branches: {
            only: ['master', '/^d+.d+.x$/', '/.*merge.*/'],
          },
        },
      }),
      new WorkflowJob(setUpJob, {
        context,
      }),
      new WorkflowJob(validateJob, {
        context,
        requires: ['setup'],
      }),
      new WorkflowJob(buildJob, {
        context,
        requires: ['validate'],
      }),
      new WorkflowJob(buildImagesJob, {
        context,
        requires: ['build'],
        filters: {
          branches: {
            only: ['master', '/^d+.d+.x$/', '/.*merge.*/', '/.*-run-e2e.*/'],
          },
        },
      }),
      new WorkflowJob(testJob, {
        context,
        requires: ['build'],
      }),
      new WorkflowJob(sonarCloudAnalysisJob, {
        context,
        requires: ['test'],
        matrix: {
          working_directory: ['gravitee-apim-rest-api', 'gravitee-apim-gateway', 'gravitee-apim-definition'],
        },
      }),
      new WorkflowJob(testPluginJob, {
        context,
        requires: ['build'],
      }),
      new WorkflowJob(sonarCloudAnalysisJob, {
        context,
        requires: ['test-plugin'],
        working_directory: 'gravitee-apim-plugin',
      }),
      new WorkflowJob(testRepositoryJob, {
        context,
        requires: ['build'],
      }),
      new WorkflowJob(sonarCloudAnalysisJob, {
        context,
        requires: ['test-repository'],
        working_directory: 'gravitee-apim-repository',
      }),
    ]),
  );

await fs.writeFile(outputFile, config.stringify());
