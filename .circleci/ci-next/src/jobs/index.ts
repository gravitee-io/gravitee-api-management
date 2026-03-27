/**
 * Job definitions for Gravitee APIM CI — pull-requests workflow.
 */
import { job, type JobDefinition } from '../sdk/job.js';
import {
  run, checkout, cache, workspace, storeArtifacts, storeTestResults, setupRemoteDocker,
  type CommandStep,
} from '../sdk/commands.js';
import { useOrb } from '../sdk/orbs.js';
import { use } from '../sdk/reusable.js';
import { param } from '../sdk/parameters.js';
import { baseExecutor, openjdkExecutor, nodeExecutor, sonarExecutor, ubuntuExecutor } from '../executors/index.js';
import {
  restoreMavenCacheCommand, saveMavenCacheCommand, notifyOnFailureCommand,
  installYarnCommand, webuiInstallCommand, workspaceInstallCommand,
  createDockerContextCommand, dockerLoginCommand, dockerLogoutCommand,
} from '../commands/index.js';
import { keeper, aquasec, helm as helmOrb } from '../orbs/index.js';
import {
  cachePrefix, maven, secrets, components, helmVersions, dockerVersion, aqua,
} from '../config/config.js';
import type { CircleCIEnvironment } from '../config/environment.js';
import { computeImagesTag, isSupportBranchOrMaster } from '../config/branch-utils.js';

// ─── Collected reusable commands & orbs for the config ───────────────────────
// These are accumulated as jobs are created, then passed to the config builder.

export interface PipelineResources {
  commands: Map<string, ReturnType<typeof restoreMavenCacheCommand>>;
  orbs: Set<ReturnType<typeof import('../sdk/orbs.js').orb>>;
}

function getOrCreate<T>(map: Map<string, T>, key: string, factory: () => T): T {
  if (!map.has(key)) map.set(key, factory());
  return map.get(key)!;
}

// ─── Setup ───────────────────────────────────────────────────────────────────

export function createSetupJob(res: PipelineResources): JobDefinition {
  res.orbs.add(keeper);
  return job('job-setup', {
    executor: baseExecutor('small'),
    steps: [
      checkout(),
      useOrb(keeper, 'env-export', { 'secret-url': secrets.mavenSettings, 'var-name': 'MAVEN_SETTINGS' }),
      run({ command: `echo $MAVEN_SETTINGS > ${maven.settingsFile} ` }),
      workspace.persist({ root: '.', paths: [maven.settingsFile] }),
    ],
  });
}

// ─── Validate backend ────────────────────────────────────────────────────────

export function createValidateJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  const restoreCmd = getOrCreate(res.commands, 'cmd-restore-maven-job-cache', () => restoreMavenCacheCommand(env));
  const saveCmd = getOrCreate(res.commands, 'cmd-save-maven-job-cache', () => saveMavenCacheCommand());
  const notifyCmd = getOrCreate(res.commands, 'cmd-notify-on-failure', () => notifyOnFailureCommand());
  res.orbs.add(keeper);
  const jobName = 'job-validate';
  return job(jobName, {
    executor: openjdkExecutor('small'),
    steps: [
      checkout(),
      workspace.attach({ at: '.' }),
      use(restoreCmd, { jobName }),
      run('Validate', `mvn -s ${maven.settingsFile} validate --no-transfer-progress -Dskip.validation=false -Dgravitee.archrules.skip=false -T 2C -Pall-modules`),
      use(notifyCmd),
      use(saveCmd, { jobName }),
    ],
  });
}

// ─── Build backend ───────────────────────────────────────────────────────────

export function createBuildBackendJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  const restoreCmd = getOrCreate(res.commands, 'cmd-restore-maven-job-cache', () => restoreMavenCacheCommand(env));
  const saveCmd = getOrCreate(res.commands, 'cmd-save-maven-job-cache', () => saveMavenCacheCommand());
  const notifyCmd = getOrCreate(res.commands, 'cmd-notify-on-failure', () => notifyOnFailureCommand());
  res.orbs.add(keeper);
  const jobName = 'job-build';
  return job(jobName, {
    executor: openjdkExecutor('large'),
    steps: [
      checkout(),
      workspace.attach({ at: '.' }),
      use(restoreCmd, { jobName }),
      run('Build project', {
        command: `mvn -s ${maven.settingsFile} clean install --no-transfer-progress --update-snapshots -DskipTests -Dskip.validation=true -Dgravitee.archrules.skip=false -T 2C -Dbundle=dev -P all-modules,integration-tests-modules -DwithJavadoc`,
        environment: {
          BUILD_ID: env.buildId,
          BUILD_NUMBER: env.buildNum,
          GIT_COMMIT: env.sha1,
        },
      }),
      use(notifyCmd),
      cache.save({
        paths: ['~/.m2/repository/io/gravitee/apim'],
        key: `${cachePrefix}-build-apim-{{ .Environment.CIRCLE_WORKFLOW_WORKSPACE_ID }}`,
        when: 'on_success',
      }),
      use(saveCmd, { jobName }),
      workspace.persist({
        root: './',
        paths: [
          './gravitee-apim-rest-api/gravitee-apim-rest-api-management/gravitee-apim-rest-api-management-rest/target/classes/console-openapi.*',
          './gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target/distribution',
          './gravitee-apim-gateway/gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-distribution/target/distribution',
        ],
      }),
    ],
  });
}

// ─── Backend test jobs (common pattern) ──────────────────────────────────────

function createBackendTestJob(
  res: PipelineResources,
  env: CircleCIEnvironment,
  jobName: string,
  testCommand: string,
  executorSize: 'small' | 'medium' | 'large' | 'xlarge' = 'small',
  pathsToPersist: string[] = [],
  properties?: { parallelism?: number },
): JobDefinition {
  const restoreCmd = getOrCreate(res.commands, 'cmd-restore-maven-job-cache', () => restoreMavenCacheCommand(env));
  const saveCmd = getOrCreate(res.commands, 'cmd-save-maven-job-cache', () => saveMavenCacheCommand());
  const notifyCmd = getOrCreate(res.commands, 'cmd-notify-on-failure', () => notifyOnFailureCommand());
  res.orbs.add(keeper);
  return job(jobName, {
    executor: openjdkExecutor(executorSize),
    ...(properties?.parallelism && { parallelism: properties.parallelism }),
    steps: [
      checkout(),
      workspace.attach({ at: '.' }),
      use(restoreCmd, { jobName }),
      cache.restore({ keys: [`${cachePrefix}-build-apim-{{ .Environment.CIRCLE_WORKFLOW_WORKSPACE_ID }}`] }),
      run(`Run ${jobName.replace('job-test-', '')} tests`, testCommand),
      run({ command: `mkdir -p ~/test-results/junit/\nfind . -type f -regex ".*/target/surefire-reports/.*xml" -exec cp {} ~/test-results/junit/ \\;`, name: 'Save test results', when: 'always' }),
      use(notifyCmd),
      use(saveCmd, { jobName }),
      storeTestResults('~/test-results'),
      ...(pathsToPersist.length > 0
        ? [workspace.persist({ root: '.', paths: pathsToPersist })]
        : []),
    ],
  });
}

export function createTestDefinitionJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  return createBackendTestJob(res, env, 'job-test-definition',
    `mvn --fail-fast -s ${maven.settingsFile} test --no-transfer-progress -Ddefinition-modules -Dskip.validation=true -Dgravitee.archrules.skip=true -T 2C`,
    'small',
    ['gravitee-apim-definition/gravitee-apim-definition-coverage/target/site/jacoco-aggregate/'],
  );
}

export function createTestGatewayJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  return createBackendTestJob(res, env, 'job-test-gateway',
    `mvn --fail-fast -s ${maven.settingsFile} test --no-transfer-progress -Dgateway-modules -Dskip.validation=true -Dgravitee.archrules.skip=true -T 2C`,
    'large',
    ['gravitee-apim-gateway/gravitee-apim-gateway-coverage/target/site/jacoco-aggregate/'],
  );
}

export function createTestRestApiJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  return createBackendTestJob(res, env, 'job-test-rest-api',
    `mvn --fail-fast -s ${maven.settingsFile} test --no-transfer-progress -Drest-api-modules -Dskip.validation=true -Dgravitee.archrules.skip=true`,
    'xlarge',
    ['gravitee-apim-rest-api/gravitee-apim-rest-api-coverage/target/site/jacoco-aggregate/'],
  );
}

export function createTestPluginJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  return createBackendTestJob(res, env, 'job-test-plugin',
    `mvn --fail-fast -s ${maven.settingsFile} test --no-transfer-progress -Dplugin-modules -Dskip.validation=true -Dgravitee.archrules.skip=true -T 2C`,
    'large',
    ['gravitee-apim-plugin/gravitee-apim-plugin-coverage/target/site/jacoco-aggregate/'],
  );
}

export function createTestReporterJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  return createBackendTestJob(res, env, 'job-test-reporter',
    `mvn --fail-fast -s ${maven.settingsFile} test --no-transfer-progress -Dreporter-modules -Dskip.validation=true -Dgravitee.archrules.skip=true -T 2C`,
    'small',
    ['gravitee-apim-reporter/gravitee-apim-reporter-coverage/target/site/jacoco-aggregate/'],
  );
}

export function createTestRepositoryJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  return createBackendTestJob(res, env, 'job-test-repository',
    `mvn --fail-fast -s ${maven.settingsFile} verify --no-transfer-progress -Drepository-modules -Dskip.validation=true -Dgravitee.archrules.skip=true`,
    'large',
    ['gravitee-apim-repository/gravitee-apim-repository-coverage/target/site/jacoco-aggregate/'],
  );
}

export function createTestIntegrationJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  return createBackendTestJob(res, env, 'job-test-integration',
    `mvn --fail-fast -s ${maven.settingsFile} test --no-transfer-progress -Dintegration-test-modules -Dskip.validation=true -Dgravitee.archrules.skip=true`,
    'xlarge',
    [],
    { parallelism: 3 },
  );
}

// ─── SonarCloud analysis ─────────────────────────────────────────────────────

export function createSonarCloudJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  const restoreCmd = getOrCreate(res.commands, 'cmd-restore-maven-job-cache', () => restoreMavenCacheCommand(env));
  const saveCmd = getOrCreate(res.commands, 'cmd-save-maven-job-cache', () => saveMavenCacheCommand());
  const notifyCmd = getOrCreate(res.commands, 'cmd-notify-on-failure', () => notifyOnFailureCommand());
  res.orbs.add(keeper);

  return job('job-sonarcloud-analysis', {
    executor: sonarExecutor('large'),
    parameters: {
      working_directory: param.string('gravitee-apim-rest-api', 'Directory where the Sonarcloud analysis will be run'),
      cache_type: param.enum('backend', ['backend', 'frontend'], 'Type of cache to use'),
    },
    steps: [
      checkout(),
      workspace.attach({ at: '.' }),
      cache.restore({ keys: [`${cachePrefix}-sonarcloud-analysis-<< parameters.cache_type >>`] }),
      useOrb(keeper, 'env-export', { 'secret-url': secrets.sonarToken, 'var-name': 'SONAR_TOKEN' }),
      run('Run Sonarcloud Analysis', {
        command: 'sonar-scanner',
        workingDirectory: '<< parameters.working_directory >>',
      }),
      use(notifyCmd),
      cache.save({
        paths: ['/opt/sonar-scanner/.sonar/cache'],
        key: `${cachePrefix}-sonarcloud-analysis-<< parameters.cache_type >>`,
        when: 'always',
      }),
    ],
  });
}

// ─── Danger JS ───────────────────────────────────────────────────────────────

export function createDangerJsJob(res: PipelineResources): JobDefinition {
  const installYarnCmd = getOrCreate(res.commands, 'cmd-install-yarn', () => installYarnCommand());
  res.orbs.add(keeper);
  return job('job-danger-js', {
    executor: nodeExecutor('small'),
    steps: [
      checkout(),
      use(installYarnCmd),
      useOrb(keeper, 'env-export', { 'secret-url': secrets.githubApiToken, 'var-name': 'DANGER_GITHUB_API_TOKEN' }),
      run('Run Danger JS', 'cd .circleci/danger && yarn install && yarn run danger'),
    ],
  });
}

// ─── Helm chart test ─────────────────────────────────────────────────────────

export function createTestApimChartsJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  res.orbs.add(helmOrb);
  res.orbs.add(keeper);
  const notifyCmd = getOrCreate(res.commands, 'cmd-notify-on-failure', () => notifyOnFailureCommand());
  return job('job-test-apim-charts', {
    executor: baseExecutor('small'),
    steps: [
      checkout(),
      useOrb(helmOrb, 'install_helm_client', { version: helmVersions.defaultVersion }),
      run('Install helm unittest plugin', `helm plugin install https://github.com/helm-unittest/helm-unittest --version ${helmVersions.helmUnitVersion}`),
      run('Helm lint', 'helm lint helm/apim3 && helm lint helm/apim4'),
      run('Helm unittest', 'helm unittest helm/apim3 && helm unittest helm/apim4'),
      use(notifyCmd),
    ],
  });
}

// ─── NX format check ────────────────────────────────────────────────────────

export function createNxFormatCheckJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  const installYarnCmd = getOrCreate(res.commands, 'cmd-install-yarn', () => installYarnCommand());
  const wsInstallCmd = getOrCreate(res.commands, 'cmd-workspace-install', () => workspaceInstallCommand());
  return job('job-nx-format-check', {
    executor: nodeExecutor('medium'),
    steps: [
      checkout(),
      use(installYarnCmd),
      use(wsInstallCmd),
      run('Check prettier formatting', 'yarn nx format:check --all'),
    ],
  });
}

// ─── WebUI lint & test ───────────────────────────────────────────────────────

export function createWebuiLintTestJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  const installYarnCmd = getOrCreate(res.commands, 'cmd-install-yarn', () => installYarnCommand());
  const webuiInstallCmd = getOrCreate(res.commands, 'cmd-webui-install', () => webuiInstallCommand());
  const notifyCmd = getOrCreate(res.commands, 'cmd-notify-on-failure', () => notifyOnFailureCommand());
  res.orbs.add(keeper);
  return job('job-webui-lint-test', {
    executor: nodeExecutor('<< parameters.resource_class >>'),
    parameters: {
      'apim-ui-project': param.string('', 'the directory name of the UI project'),
      resource_class: param.string('medium', 'Resource class to use for executor'),
    },
    steps: [
      checkout(),
      use(installYarnCmd),
      use(webuiInstallCmd, { 'apim-ui-project': '<< parameters.apim-ui-project >>' }),
      workspace.attach({ at: '.' }),
      run('Check License', { command: 'yarn lint:license', workingDirectory: '<< parameters.apim-ui-project >>' }),
      run('Run Prettier and ESLint', { command: 'yarn lint', workingDirectory: '<< parameters.apim-ui-project >>' }),
      run('Run unit tests', { command: 'yarn test:coverage', workingDirectory: '<< parameters.apim-ui-project >>' }),
      use(notifyCmd),
      workspace.persist({ root: '.', paths: ['<< parameters.apim-ui-project >>/coverage/lcov.info'] }),
      storeArtifacts({ path: '<< parameters.apim-ui-project >>/coverage/lcov.info' }),
      storeTestResults('<< parameters.apim-ui-project >>/coverage/junit.xml'),
    ],
  });
}

export function createWebuiNxLintTestJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  const installYarnCmd = getOrCreate(res.commands, 'cmd-install-yarn', () => installYarnCommand());
  const wsInstallCmd = getOrCreate(res.commands, 'cmd-workspace-install', () => workspaceInstallCommand());
  const notifyCmd = getOrCreate(res.commands, 'cmd-notify-on-failure', () => notifyOnFailureCommand());
  res.orbs.add(keeper);
  return job('job-nx-webui-lint-test', {
    executor: nodeExecutor('<< parameters.resource_class >>'),
    parameters: {
      'apim-ui-project': param.string('', 'the directory name of the UI project'),
      'nx-project': param.string('', 'the Nx project name'),
      resource_class: param.string('medium', 'Resource class to use for executor'),
      'max-workers': param.string('35%', 'Maximum number of workers for Jest tests'),
    },
    steps: [
      checkout(),
      use(installYarnCmd),
      use(wsInstallCmd),
      workspace.attach({ at: '.' }),
      run('Check License', 'yarn nx run << parameters.nx-project >>:lint-license'),
      run('Run Prettier and ESLint', 'yarn nx lint << parameters.nx-project >>'),
      run('Run unit tests', 'yarn nx test << parameters.nx-project >> --coverage --maxWorkers=<< parameters.max-workers >>'),
      use(notifyCmd),
      workspace.persist({ root: '.', paths: ['<< parameters.apim-ui-project >>/coverage/lcov.info'] }),
      storeArtifacts({ path: '<< parameters.apim-ui-project >>/coverage/lcov.info' }),
      storeTestResults('<< parameters.apim-ui-project >>/coverage/junit.xml'),
    ],
  });
}

export function createWebuiLibsLintTestJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  const installYarnCmd = getOrCreate(res.commands, 'cmd-install-yarn', () => installYarnCommand());
  const wsInstallCmd = getOrCreate(res.commands, 'cmd-workspace-install', () => workspaceInstallCommand());
  const notifyCmd = getOrCreate(res.commands, 'cmd-notify-on-failure', () => notifyOnFailureCommand());
  res.orbs.add(keeper);
  return job('job-webui-libs-lint-test', {
    executor: nodeExecutor('medium'),
    steps: [
      checkout(),
      use(installYarnCmd),
      use(wsInstallCmd),
      workspace.attach({ at: '.' }),
      run('Lint APIM Libs (affected)', 'yarn nx affected -t lint --exclude=console,portal-next'),
      run('Test APIM Libs (affected)', 'NODE_OPTIONS=--max-old-space-size=4096 yarn nx affected -t test --exclude=console,portal-next --coverage --maxWorkers=2'),
      use(notifyCmd),
      run('Ensure coverage files exist', [
        'mkdir -p gravitee-apim-webui-libs/gravitee-markdown/coverage',
        'mkdir -p gravitee-apim-webui-libs/gravitee-dashboard/coverage',
        'touch gravitee-apim-webui-libs/gravitee-markdown/coverage/lcov.info',
        'touch gravitee-apim-webui-libs/gravitee-dashboard/coverage/lcov.info',
      ].join('\n')),
      workspace.persist({ root: '.', paths: ['gravitee-apim-webui-libs/gravitee-markdown/coverage/lcov.info'] }),
      storeArtifacts({ path: 'gravitee-apim-webui-libs/gravitee-markdown/coverage/lcov.info' }),
      storeTestResults('gravitee-apim-webui-libs/gravitee-markdown/coverage/junit.xml'),
      workspace.persist({ root: '.', paths: ['gravitee-apim-webui-libs/gravitee-dashboard/coverage/lcov.info'] }),
      storeArtifacts({ path: 'gravitee-apim-webui-libs/gravitee-dashboard/coverage/lcov.info' }),
      storeTestResults('gravitee-apim-webui-libs/gravitee-dashboard/coverage/junit.xml'),
    ],
  });
}

// ─── Console build ───────────────────────────────────────────────────────────

export function createConsoleWebuiBuildJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  const installYarnCmd = getOrCreate(res.commands, 'cmd-install-yarn', () => installYarnCommand());
  const wsInstallCmd = getOrCreate(res.commands, 'cmd-workspace-install', () => workspaceInstallCommand());
  const notifyCmd = getOrCreate(res.commands, 'cmd-notify-on-failure', () => notifyOnFailureCommand());
  res.orbs.add(keeper);
  return job('job-console-webui-build', {
    executor: nodeExecutor('large'),
    steps: [
      checkout(),
      use(installYarnCmd),
      use(wsInstallCmd),
      workspace.attach({ at: '.' }),
      run('Build Console', 'yarn nx build console --configuration=production'),
      use(notifyCmd),
      workspace.persist({ root: '.', paths: [`${components.console.project}/dist/`] }),
    ],
  });
}

// ─── Portal build ────────────────────────────────────────────────────────────

export function createPortalWebuiBuildJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  const installYarnCmd = getOrCreate(res.commands, 'cmd-install-yarn', () => installYarnCommand());
  const wsInstallCmd = getOrCreate(res.commands, 'cmd-workspace-install', () => workspaceInstallCommand());
  const notifyCmd = getOrCreate(res.commands, 'cmd-notify-on-failure', () => notifyOnFailureCommand());
  res.orbs.add(keeper);
  return job('job-portal-webui-build', {
    executor: nodeExecutor('large'),
    steps: [
      checkout(),
      use(installYarnCmd),
      use(wsInstallCmd),
      workspace.attach({ at: '.' }),
      run('Build Portal', {
        command: 'yarn build',
        workingDirectory: components.portal.project,
      }),
      run('Build Portal Next', 'yarn nx build portal-next --configuration=production'),
      use(notifyCmd),
      workspace.persist({
        root: '.',
        paths: [`${components.portal.project}/dist/`, `${components.portal.next.project}/dist/`],
      }),
    ],
  });
}

// ─── Storybook ───────────────────────────────────────────────────────────────

export function createStorybookConsoleJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  const installYarnCmd = getOrCreate(res.commands, 'cmd-install-yarn', () => installYarnCommand());
  const wsInstallCmd = getOrCreate(res.commands, 'cmd-workspace-install', () => workspaceInstallCommand());
  const notifyCmd = getOrCreate(res.commands, 'cmd-notify-on-failure', () => notifyOnFailureCommand());
  res.orbs.add(keeper);
  return job('job-storybook-console', {
    executor: nodeExecutor('large'),
    steps: [
      checkout(),
      use(installYarnCmd),
      use(wsInstallCmd),
      workspace.attach({ at: '.' }),
      run('Build Storybook', 'yarn nx run console:build-storybook'),
      use(notifyCmd),
      workspace.persist({ root: '.', paths: [`${components.console.project}/storybook-static/`] }),
    ],
  });
}

export function createChromaticConsoleJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  const installYarnCmd = getOrCreate(res.commands, 'cmd-install-yarn', () => installYarnCommand());
  const wsInstallCmd = getOrCreate(res.commands, 'cmd-workspace-install', () => workspaceInstallCommand());
  res.orbs.add(keeper);
  return job('job-chromatic-console', {
    executor: nodeExecutor('small'),
    steps: [
      checkout(),
      use(installYarnCmd),
      use(wsInstallCmd),
      workspace.attach({ at: '.' }),
      useOrb(keeper, 'env-export', { 'secret-url': secrets.chromaticProjectToken, 'var-name': 'CHROMATIC_PROJECT_TOKEN' }),
      run('Deploy to Chromatic', {
        command: 'yarn nx run console:chromatic --exit-zero-on-changes --exit-once-uploaded --storybook-build-dir=storybook-static',
        workingDirectory: components.console.project,
      }),
    ],
  });
}

// ─── Docker image builds ─────────────────────────────────────────────────────

export function createBuildDockerWebUiImageJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  const createContextCmd = getOrCreate(res.commands, 'cmd-create-docker-context', () => createDockerContextCommand());
  const loginCmd = getOrCreate(res.commands, 'cmd-docker-login', () => dockerLoginCommand(env, false));
  const logoutCmd = getOrCreate(res.commands, 'cmd-docker-logout', () => dockerLogoutCommand(env, false));
  res.orbs.add(keeper);
  res.orbs.add(aquasec);

  const tag = computeImagesTag(env.branch, env.sha1);
  return job('job-build-docker-webui-image', {
    executor: baseExecutor(),
    parameters: {
      'apim-project': param.string('', 'the name of the project to build'),
      'docker-context': param.string('', 'the name of context folder for docker build'),
      'docker-image-name': param.string('', 'the name of the image'),
    },
    steps: [
      checkout(),
      workspace.attach({ at: '.' }),
      setupRemoteDocker({ version: dockerVersion }),
      use(createContextCmd),
      use(loginCmd),
      run('Build docker image for << parameters.apim-project >>', {
        command: `docker buildx build --push --platform=linux/arm64,linux/amd64 -f docker/Dockerfile \\\n-t graviteeio.azurecr.io/<< parameters.docker-image-name >>:${tag} \\\n<< parameters.docker-context >>`,
        workingDirectory: '<< parameters.apim-project >>',
      }),
      use(logoutCmd),
    ],
  });
}

export function createBuildDockerBackendImageJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  const createContextCmd = getOrCreate(res.commands, 'cmd-create-docker-context', () => createDockerContextCommand());
  const loginCmd = getOrCreate(res.commands, 'cmd-docker-login', () => dockerLoginCommand(env, false));
  const logoutCmd = getOrCreate(res.commands, 'cmd-docker-logout', () => dockerLogoutCommand(env, false));
  res.orbs.add(keeper);
  res.orbs.add(aquasec);

  const tag = computeImagesTag(env.branch, env.sha1);
  const variants = ['alpine', 'debian'] as const;
  const buildSteps: CommandStep[] = variants.flatMap((variant) => {
    const dockerfile = variant === 'debian' ? 'docker/Dockerfile.debian' : 'docker/Dockerfile';
    const suffix = variant === 'debian' ? '-debian' : '';
    return [
      run(`Build docker image for << parameters.apim-project >>-${variant}`, {
        command: `docker buildx build --push --platform=linux/arm64,linux/amd64 -f ${dockerfile} \\\n-t graviteeio.azurecr.io/<< parameters.docker-image-name >>:${tag}${suffix} \\\n<< parameters.docker-context >>`,
        workingDirectory: '<< parameters.apim-project >>',
      }),
    ];
  });

  return job('job-build-docker-backend-image', {
    executor: baseExecutor(),
    parameters: {
      'apim-project': param.string('', 'the name of the project to build'),
      'docker-context': param.string('', 'the name of context folder for docker build'),
      'docker-image-name': param.string('', 'the name of the image'),
    },
    steps: [
      checkout(),
      workspace.attach({ at: '.' }),
      setupRemoteDocker({ version: dockerVersion }),
      use(createContextCmd),
      use(loginCmd),
      ...buildSteps,
      use(logoutCmd),
    ],
  });
}

// ─── E2E jobs ────────────────────────────────────────────────────────────────

export function createE2EGenerateSDKJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  const installYarnCmd = getOrCreate(res.commands, 'cmd-install-yarn', () => installYarnCommand());
  const webuiInstallCmd = getOrCreate(res.commands, 'cmd-webui-install', () => webuiInstallCommand());
  const notifyCmd = getOrCreate(res.commands, 'cmd-notify-on-failure', () => notifyOnFailureCommand());
  res.orbs.add(keeper);
  return job('job-e2e-generate-sdk', {
    executor: openjdkExecutor('small'),
    steps: [
      checkout(),
      workspace.attach({ at: '.' }),
      use(installYarnCmd),
      run('Generate e2e tests SDK', {
        command: 'sh ./scripts/update-management-sdk.sh\nsh ./scripts/update-management-v2-sdk.sh\nsh ./scripts/update-portal-sdk.sh',
        workingDirectory: 'gravitee-apim-e2e',
      }),
      use(webuiInstallCmd, { 'apim-ui-project': 'gravitee-apim-e2e' }),
      use(notifyCmd),
      workspace.persist({
        root: '.',
        paths: [
          'gravitee-apim-e2e/lib/management-webclient-sdk',
          'gravitee-apim-e2e/lib/management-v2-webclient-sdk',
          'gravitee-apim-e2e/lib/portal-webclient-sdk',
        ],
      }),
    ],
  });
}

export function createE2ELintBuildJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  const installYarnCmd = getOrCreate(res.commands, 'cmd-install-yarn', () => installYarnCommand());
  const webuiInstallCmd = getOrCreate(res.commands, 'cmd-webui-install', () => webuiInstallCommand());
  const notifyCmd = getOrCreate(res.commands, 'cmd-notify-on-failure', () => notifyOnFailureCommand());
  res.orbs.add(keeper);
  return job('job-e2e-lint-build', {
    executor: nodeExecutor('small'),
    steps: [
      checkout(),
      workspace.attach({ at: '.' }),
      use(installYarnCmd),
      use(webuiInstallCmd, { 'apim-ui-project': 'gravitee-apim-e2e' }),
      run('Lint', { command: 'yarn lint', workingDirectory: 'gravitee-apim-e2e' }),
      run('Build', { command: 'yarn build', workingDirectory: 'gravitee-apim-e2e' }),
      use(notifyCmd),
      workspace.persist({ root: '.', paths: ['gravitee-apim-e2e/dist/'] }),
    ],
  });
}

export function createE2ETestJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  const installYarnCmd = getOrCreate(res.commands, 'cmd-install-yarn', () => installYarnCommand());
  const loginCmd = getOrCreate(res.commands, 'cmd-docker-login', () => dockerLoginCommand(env, false));
  const logoutCmd = getOrCreate(res.commands, 'cmd-docker-logout', () => dockerLogoutCommand(env, false));
  const notifyCmd = getOrCreate(res.commands, 'cmd-notify-on-failure', () => notifyOnFailureCommand());
  res.orbs.add(keeper);

  const dockerImageTag = computeImagesTag(env.branch, env.sha1);

  return job('job-e2e-test', {
    executor: ubuntuExecutor(),
    parameters: {
      apim_client_tag: param.string(''),
      execution_mode: param.string(''),
      database: param.string(''),
    },
    steps: [
      checkout(),
      workspace.attach({ at: '.' }),
      use(loginCmd),
      useOrb(keeper, 'env-export', { 'secret-url': secrets.graviteeLicense, 'var-name': 'GRAVITEE_LICENSE' }),
      use(installYarnCmd),
      run('Running API & E2E tests in << parameters.execution_mode >> mode with << parameters.database >> database', {
        command: `cd gravitee-apim-e2e
if [ "<< parameters.execution_mode >>" = "v3" ]; then
  echo "Disable v4 emulation engine on APIM Gateway and Rest API"
  export V4_EMULATION_ENGINE_DEFAULT=no
fi
if [ -z "<< parameters.apim_client_tag >>" ]; then
  APIM_REGISTRY=graviteeio.azurecr.io APIM_TAG=${dockerImageTag} APIM_CLIENT_REGISTRY=graviteeio.azurecr.io APIM_CLIENT_TAG=${dockerImageTag} yarn test:api:<< parameters.database >>
else
  if [[ "<< parameters.apim_client_tag >>" == *"@"* ]]; then
    echo "Using custom registry for client"
    CLIENT_REGISTRY=$(echo "<< parameters.apim_client_tag >>" | cut -f1 -d@)
    CLIENT_TAG=$(echo "<< parameters.apim_client_tag >>" | cut -f2 -d@)
    APIM_REGISTRY=graviteeio.azurecr.io APIM_TAG=${dockerImageTag} APIM_CLIENT_REGISTRY=\${CLIENT_REGISTRY} APIM_CLIENT_TAG=\${CLIENT_TAG} yarn test:api:<< parameters.database >>
  else
    echo "Using ACR registry for client"
    APIM_REGISTRY=graviteeio.azurecr.io APIM_TAG=${dockerImageTag} APIM_CLIENT_REGISTRY=graviteeio.azurecr.io APIM_CLIENT_TAG=<< parameters.apim_client_tag >> yarn test:api:<< parameters.database >>
  fi
fi`,
      }),
      use(logoutCmd),
      use(notifyCmd),
      storeTestResults('./gravitee-apim-e2e/.tmp/e2e-test-report.xml'),
      storeArtifacts({ path: './gravitee-apim-e2e/.tmp/e2e-test-report.xml' }),
      storeArtifacts({ path: './gravitee-apim-e2e/.logs' }),
    ],
  });
}

export function createE2ECypressJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  const installYarnCmd = getOrCreate(res.commands, 'cmd-install-yarn', () => installYarnCommand());
  const loginCmd = getOrCreate(res.commands, 'cmd-docker-login', () => dockerLoginCommand(env, false));
  const logoutCmd = getOrCreate(res.commands, 'cmd-docker-logout', () => dockerLogoutCommand(env, false));
  const notifyCmd = getOrCreate(res.commands, 'cmd-notify-on-failure', () => notifyOnFailureCommand());
  res.orbs.add(keeper);

  const dockerImageTag = computeImagesTag(env.branch, env.sha1);

  return job('job-e2e-cypress', {
    executor: ubuntuExecutor(),
    steps: [
      checkout(),
      workspace.attach({ at: '.' }),
      use(loginCmd),
      useOrb(keeper, 'env-export', { 'secret-url': secrets.graviteeLicense, 'var-name': 'GRAVITEE_LICENSE' }),
      useOrb(keeper, 'env-export', { 'secret-url': secrets.cypressCloudKey, 'var-name': 'CYPRESS_RECORD_KEY' }),
      use(installYarnCmd),
      run('Run Cypress UI tests', {
        command: `cd gravitee-apim-e2e\nAPIM_REGISTRY=graviteeio.azurecr.io APIM_TAG=${dockerImageTag} yarn test:ui`,
      }),
      use(logoutCmd),
      use(notifyCmd),
      storeTestResults('./gravitee-apim-e2e/.tmp/cypress-results/'),
      storeArtifacts({ path: './gravitee-apim-e2e/.tmp/cypress-results/' }),
      storeArtifacts({ path: './gravitee-apim-e2e/.logs' }),
    ],
  });
}

// ─── Perf lint & build ───────────────────────────────────────────────────────

export function createPerfLintBuildJob(res: PipelineResources, env: CircleCIEnvironment): JobDefinition {
  const installYarnCmd = getOrCreate(res.commands, 'cmd-install-yarn', () => installYarnCommand());
  const webuiInstallCmd = getOrCreate(res.commands, 'cmd-webui-install', () => webuiInstallCommand());
  const notifyCmd = getOrCreate(res.commands, 'cmd-notify-on-failure', () => notifyOnFailureCommand());
  res.orbs.add(keeper);
  return job('job-perf-lint-build', {
    executor: nodeExecutor('small'),
    steps: [
      checkout(),
      workspace.attach({ at: '.' }),
      use(installYarnCmd),
      use(webuiInstallCmd, { 'apim-ui-project': 'gravitee-apim-perf' }),
      run('Lint', { command: 'yarn lint', workingDirectory: 'gravitee-apim-perf' }),
      run('Build', { command: 'yarn build', workingDirectory: 'gravitee-apim-perf' }),
      use(notifyCmd),
    ],
  });
}
