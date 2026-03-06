/**
 * Pull-requests workflow — rebuilt with the new SDK.
 *
 * Equivalent to: .circleci/ci/src/workflows/workflow-pull-requests.ts
 */
import { config, type Config } from '../sdk/config.js';
import { job } from '../sdk/job.js';
import { workflow, wfJob, type WorkflowJobEntry } from '../sdk/workflow.js';
import { run } from '../sdk/commands.js';
import { orbJob } from '../sdk/orbs.js';
import { useOrb } from '../sdk/orbs.js';
import { baseExecutor } from '../executors/index.js';
import { keeper, aquasec } from '../orbs/index.js';
import { jobContext, secrets, components } from '../config/config.js';
import { isE2EBranch, isMasterBranch, isSupportBranchOrMaster } from '../config/branch-utils.js';
import type { CircleCIEnvironment } from '../config/environment.js';
import type { PipelineResources } from '../jobs/index.js';
import {
  createSetupJob,
  createValidateJob,
  createBuildBackendJob,
  createTestDefinitionJob,
  createTestGatewayJob,
  createTestRestApiJob,
  createTestPluginJob,
  createTestReporterJob,
  createTestRepositoryJob,
  createTestIntegrationJob,
  createSonarCloudJob,
  createDangerJsJob,
  createTestApimChartsJob,
  createNxFormatCheckJob,
  createWebuiLintTestJob,
  createWebuiNxLintTestJob,
  createWebuiLibsLintTestJob,
  createConsoleWebuiBuildJob,
  createPortalWebuiBuildJob,
  createStorybookConsoleJob,
  createChromaticConsoleJob,
  createBuildDockerWebUiImageJob,
  createBuildDockerBackendImageJob,
  createE2EGenerateSDKJob,
  createE2ELintBuildJob,
  createE2ETestJob,
  createE2ECypressJob,
  createPerfLintBuildJob,
} from '../jobs/index.js';

// ─── Changed-file detection ──────────────────────────────────────────────────

function shouldBuildAll(changedFiles: string[]): boolean {
  const ids = ['.circleci', 'pom.xml', '.gitignore', '.prettierrc', 'gravitee-apim-e2e'];
  return changedFiles.some((f) => ids.some((id) => f.includes(id)));
}

function shouldBuildAllFront(changedFiles: string[]): boolean {
  const ids = ['package.json', 'nx.json', 'yarn.lock'];
  return shouldBuildAll(changedFiles) || changedFiles.some((f) => ids.some((id) => f.includes(id)));
}

function shouldBuildHelm(changedFiles: string[]): boolean {
  return shouldBuildAll(changedFiles) || changedFiles.some((f) => f.includes('helm'));
}

function shouldBuildWebuiLibs(changedFiles: string[]): boolean {
  return shouldBuildAllFront(changedFiles) || changedFiles.some((f) => f.includes('gravitee-apim-webui-libs'));
}

function shouldBuildConsole(changedFiles: string[]): boolean {
  return (
    shouldBuildAllFront(changedFiles) ||
    changedFiles.some((f) => f.includes(components.console.project)) ||
    changedFiles.some((f) => f.includes('gravitee-apim-webui-libs'))
  );
}

function shouldBuildPortalNext(changedFiles: string[]): boolean {
  return (
    shouldBuildAllFront(changedFiles) ||
    changedFiles.some((f) => f.includes(components.portal.next.project)) ||
    changedFiles.some((f) => f.includes('gravitee-apim-webui-libs'))
  );
}

function shouldBuildPortal(changedFiles: string[]): boolean {
  return (
    shouldBuildAllFront(changedFiles) ||
    changedFiles.some((f) => f.includes(components.portal.project) && !f.includes(components.portal.next.project))
  );
}

function shouldBuildBackend(changedFiles: string[]): boolean {
  const ids = [
    'pom.xml', 'gravitee-apim-bom', 'gravitee-apim-common', 'gravitee-apim-definition',
    'gravitee-apim-distribution', 'gravitee-apim-gateway', 'gravitee-apim-integration-tests',
    'gravitee-apim-parent', 'gravitee-apim-plugin', 'gravitee-apim-reporter',
    'gravitee-apim-repository', 'gravitee-apim-rest-api',
  ];
  return shouldBuildAll(changedFiles) || changedFiles.some((f) => ids.some((id) => f.includes(id)));
}

function shouldTestAllBackend(changedFiles: string[]): boolean {
  const ids = [
    'pom.xml', 'gravitee-apim-bom', 'gravitee-apim-common', 'gravitee-apim-definition',
    'gravitee-apim-parent', 'gravitee-apim-repository',
  ];
  return shouldBuildAll(changedFiles) || changedFiles.some((f) => ids.some((id) => f.includes(id)));
}

function shouldTestDefinition(cf: string[]): boolean {
  return shouldTestAllBackend(cf) || cf.some((f) => f.includes('gravitee-apim-definition'));
}

function shouldTestGateway(cf: string[]): boolean {
  const ids = ['gravitee-apim-definition', 'gravitee-apim-repository', 'gravitee-apim-gateway'];
  return shouldTestAllBackend(cf) || cf.some((f) => ids.some((id) => f.includes(id)));
}

function shouldTestRestApi(cf: string[]): boolean {
  const ids = ['gravitee-apim-definition', 'gravitee-apim-repository', 'gravitee-apim-rest-api'];
  return shouldTestAllBackend(cf) || cf.some((f) => ids.some((id) => f.includes(id)));
}

function shouldTestPlugin(cf: string[]): boolean {
  const ids = ['gravitee-apim-definition', 'gravitee-apim-plugin'];
  return shouldTestAllBackend(cf) || cf.some((f) => ids.some((id) => f.includes(id)));
}

function shouldTestReporter(cf: string[]): boolean {
  return shouldTestAllBackend(cf) || cf.some((f) => f.includes('gravitee-apim-reporter'));
}

function shouldTestRepository(cf: string[]): boolean {
  const ids = ['gravitee-apim-definition', 'gravitee-apim-repository'];
  return shouldTestAllBackend(cf) || cf.some((f) => ids.some((id) => f.includes(id)));
}

function shouldTestIntegrationTests(cf: string[]): boolean {
  const ids = [
    'gravitee-apim-bom', 'gravitee-apim-common', 'gravitee-apim-definition',
    'gravitee-apim-gateway', 'gravitee-apim-integration-tests', 'gravitee-apim-parent',
    'gravitee-apim-plugin', 'gravitee-apim-reporter',
  ];
  return shouldTestAllBackend(cf) || cf.some((f) => ids.some((id) => f.includes(id)));
}

// ─── Workflow builders ───────────────────────────────────────────────────────

function getCommonJobs(
  res: PipelineResources,
  env: CircleCIEnvironment,
  filterJobs: boolean,
  addValidationJob: boolean,
  shouldBuildDockerImages: boolean,
): WorkflowJobEntry[] {
  res.orbs.add(keeper);
  res.orbs.add(aquasec);

  const dangerJsJob = createDangerJsJob(res);

  const jobs: WorkflowJobEntry[] = [
    wfJob(orbJob(aquasec, 'fs_scan'), {
      context: jobContext,
      'pre-steps': [
        useOrb(keeper, 'env-export', { 'secret-url': secrets.aquaKey, 'var-name': 'AQUA_KEY' }),
        useOrb(keeper, 'env-export', { 'secret-url': secrets.aquaSecret, 'var-name': 'AQUA_SECRET' }),
        useOrb(keeper, 'env-export', { 'secret-url': secrets.githubApiToken, 'var-name': 'GITHUB_TOKEN' }),
      ],
    }),
    wfJob(dangerJsJob, { name: 'Run Danger JS', context: jobContext }),
  ];

  const requires: string[] = [];

  if (!filterJobs || shouldBuildHelm(env.changedFiles)) {
    const apimChartsTestJob = createTestApimChartsJob(res, env);
    jobs.push(wfJob(apimChartsTestJob, { name: 'Helm Chart - Lint & Test', context: jobContext }));
    requires.push('Helm Chart - Lint & Test');
  }

  if (!filterJobs || shouldBuildBackend(env.changedFiles)) {
    const setupJob = createSetupJob(res);
    const validateBackendJob = createValidateJob(res, env);
    const buildBackendJob = createBuildBackendJob(res, env);

    jobs.push(
      wfJob(setupJob, { name: 'Setup', context: jobContext }),
      wfJob(validateBackendJob, { name: 'Validate backend', context: jobContext, requires: ['Setup'] }),
      wfJob(buildBackendJob, { name: 'Build backend', context: jobContext, requires: ['Validate backend'] }),
    );

    const sonarCloudJob = createSonarCloudJob(res, env);

    if (!filterJobs || shouldTestDefinition(env.changedFiles)) {
      const testDefJob = createTestDefinitionJob(res, env);
      jobs.push(
        wfJob(testDefJob, { name: 'Test definition', context: jobContext, requires: ['Build backend'] }),
        wfJob(sonarCloudJob, {
          name: 'Sonar - gravitee-apim-definition', context: jobContext, requires: ['Test definition'],
          working_directory: 'gravitee-apim-definition', cache_type: 'backend',
        }),
      );
      requires.push('Test definition');
    }

    if (!filterJobs || shouldTestGateway(env.changedFiles)) {
      const testGwJob = createTestGatewayJob(res, env);
      jobs.push(
        wfJob(testGwJob, { name: 'Test gateway', context: jobContext, requires: ['Build backend'] }),
        wfJob(sonarCloudJob, {
          name: 'Sonar - gravitee-apim-gateway', context: jobContext, requires: ['Test gateway'],
          working_directory: 'gravitee-apim-gateway', cache_type: 'backend',
        }),
      );
      requires.push('Test gateway');
    }

    if (!filterJobs || shouldTestRestApi(env.changedFiles)) {
      const testRestJob = createTestRestApiJob(res, env);
      jobs.push(
        wfJob(testRestJob, { name: 'Test rest-api', context: jobContext, requires: ['Build backend'] }),
        wfJob(sonarCloudJob, {
          name: 'Sonar - gravitee-apim-rest-api', context: jobContext, requires: ['Test rest-api'],
          working_directory: 'gravitee-apim-rest-api', cache_type: 'backend',
        }),
      );
      requires.push('Test rest-api');
    }

    if (!filterJobs || shouldTestIntegrationTests(env.changedFiles)) {
      const testIntJob = createTestIntegrationJob(res, env);
      jobs.push(wfJob(testIntJob, { name: 'Integration tests', context: jobContext, requires: ['Build backend'] }));
      requires.push('Integration tests');
    }

    if (!filterJobs || shouldTestPlugin(env.changedFiles)) {
      const testPluginJob = createTestPluginJob(res, env);
      jobs.push(
        wfJob(testPluginJob, { name: 'Test plugins', context: jobContext, requires: ['Build backend'] }),
        wfJob(sonarCloudJob, {
          name: 'Sonar - gravitee-apim-plugin', context: jobContext, requires: ['Test plugins'],
          working_directory: 'gravitee-apim-plugin', cache_type: 'backend',
        }),
      );
      requires.push('Test plugins');
    }

    if (!filterJobs || shouldTestReporter(env.changedFiles)) {
      const testReporterJob = createTestReporterJob(res, env);
      jobs.push(
        wfJob(testReporterJob, { name: 'Test reporters', context: jobContext, requires: ['Build backend'] }),
        wfJob(sonarCloudJob, {
          name: 'Sonar - gravitee-apim-reporter', context: jobContext, requires: ['Test reporters'],
          working_directory: 'gravitee-apim-reporter', cache_type: 'backend',
        }),
      );
      requires.push('Test reporters');
    }

    if (!filterJobs || shouldTestRepository(env.changedFiles)) {
      const testRepoJob = createTestRepositoryJob(res, env);
      jobs.push(
        wfJob(testRepoJob, { name: 'Test repository', context: jobContext, requires: ['Build backend'] }),
        wfJob(sonarCloudJob, {
          name: 'Sonar - gravitee-apim-repository', context: jobContext, requires: ['Test repository'],
          working_directory: 'gravitee-apim-repository', cache_type: 'backend',
        }),
      );
      requires.push('Test repository');
    }
  }

  // Frontend
  if (
    !filterJobs || shouldBuildWebuiLibs(env.changedFiles) || shouldBuildConsole(env.changedFiles) ||
    shouldBuildPortalNext(env.changedFiles) || shouldBuildPortal(env.changedFiles)
  ) {
    const formatCheckJob = createNxFormatCheckJob(res, env);
    jobs.push(wfJob(formatCheckJob, { name: 'Check prettier formatting for nx projects', context: jobContext }));
    requires.push('Check prettier formatting for nx projects');
  }

  if (!filterJobs || shouldBuildWebuiLibs(env.changedFiles)) {
    const webuiLibsJob = createWebuiLibsLintTestJob(res, env);
    jobs.push(wfJob(webuiLibsJob, { name: 'Lint & test APIM Libs', context: jobContext }));
    requires.push('Lint & test APIM Libs');
  }

  if (!filterJobs || shouldBuildConsole(env.changedFiles)) {
    const webuiNxLintTestJob = createWebuiNxLintTestJob(res, env);
    const consoleBuildJob = createConsoleWebuiBuildJob(res, env);
    const storybookJob = createStorybookConsoleJob(res, env);
    const chromaticJob = createChromaticConsoleJob(res, env);
    const sonarCloudJob = createSonarCloudJob(res, env);

    jobs.push(
      wfJob(webuiNxLintTestJob, {
        name: 'Lint & test APIM Console', context: jobContext,
        'apim-ui-project': components.console.project, 'nx-project': 'console',
        resource_class: 'xlarge', 'max-workers': '4',
      }),
      wfJob(consoleBuildJob, { name: 'Build APIM Console', context: jobContext }),
    );
    requires.push('Lint & test APIM Console', 'Build APIM Console');

    if (shouldBuildDockerImages) {
      const dockerWebUiJob = createBuildDockerWebUiImageJob(res, env);
      jobs.push(wfJob(dockerWebUiJob, {
        context: jobContext, name: 'Build APIM Console docker image', requires: ['Build APIM Console'],
        'apim-project': components.console.project, 'docker-context': '.', 'docker-image-name': components.console.image,
      }));
      requires.push('Build APIM Console docker image');
    }

    jobs.push(
      wfJob(storybookJob, { name: 'Build Console Storybook', context: jobContext }),
      wfJob(chromaticJob, { name: 'Deploy console in chromatic', context: jobContext, requires: ['Build Console Storybook'] }),
      wfJob(sonarCloudJob, {
        name: 'Sonar - gravitee-apim-console-webui', context: jobContext, requires: ['Lint & test APIM Console'],
        working_directory: components.console.project, cache_type: 'frontend',
      }),
    );
  }

  if (!filterJobs || shouldBuildPortalNext(env.changedFiles)) {
    const webuiNxLintTestJob = createWebuiNxLintTestJob(res, env);
    const sonarCloudJob = createSonarCloudJob(res, env);
    jobs.push(
      wfJob(webuiNxLintTestJob, {
        name: 'Lint & test APIM Portal Next', context: jobContext,
        'apim-ui-project': components.portal.next.project, 'nx-project': 'portal-next', 'max-workers': '2',
      }),
      wfJob(sonarCloudJob, {
        name: 'Sonar - gravitee-apim-portal-webui-next', context: jobContext, requires: ['Lint & test APIM Portal Next'],
        working_directory: components.portal.next.project, cache_type: 'frontend',
      }),
    );
    requires.push('Lint & test APIM Portal Next');
  }

  if (!filterJobs || shouldBuildPortal(env.changedFiles)) {
    const webuiLintTestJob = createWebuiLintTestJob(res, env);
    const sonarCloudJob = createSonarCloudJob(res, env);
    jobs.push(
      wfJob(webuiLintTestJob, {
        name: 'Lint & test APIM Portal', context: jobContext,
        'apim-ui-project': components.portal.project, resource_class: 'large',
      }),
      wfJob(sonarCloudJob, {
        name: 'Sonar - gravitee-apim-portal-webui', context: jobContext, requires: ['Lint & test APIM Portal'],
        working_directory: components.portal.project, cache_type: 'frontend',
      }),
    );
    requires.push('Lint & test APIM Portal');
  }

  if (!filterJobs || shouldBuildPortal(env.changedFiles) || shouldBuildPortalNext(env.changedFiles)) {
    const portalBuildJob = createPortalWebuiBuildJob(res, env);
    jobs.push(wfJob(portalBuildJob, { name: 'Build APIM Portal', context: jobContext }));
    requires.push('Build APIM Portal');

    if (shouldBuildDockerImages) {
      const dockerWebUiJob = createBuildDockerWebUiImageJob(res, env);
      jobs.push(wfJob(dockerWebUiJob, {
        context: jobContext, name: 'Build APIM Portal docker image', requires: ['Build APIM Portal'],
        'apim-project': components.portal.project, 'docker-context': '.', 'docker-image-name': components.portal.image,
      }));
      requires.push('Build APIM Portal docker image');
    }
  }

  // Distribution change → force validation
  if (env.changedFiles.some((f) => f.includes('gravitee-apim-distribution'))) {
    addValidationJob = true;
    requires.push('Build backend');
  }

  // Validate workflow status
  if (addValidationJob && requires.length > 0) {
    const checkJob = job('job-validate-workflow-status', {
      executor: baseExecutor('small'),
      steps: [run('Check workflow jobs', 'echo "Congratulations! If you can read this, everything is OK"')],
    });
    jobs.push(wfJob(checkJob, { name: 'Validate workflow status', requires }));
  }

  return jobs;
}

function getE2EJobs(res: PipelineResources, env: CircleCIEnvironment): WorkflowJobEntry[] {
  const dockerBackendJob = createBuildDockerBackendImageJob(res, env);
  const e2eGenSdkJob = createE2EGenerateSDKJob(res, env);
  const e2eLintBuildJob = createE2ELintBuildJob(res, env);
  const e2eTestJob = createE2ETestJob(res, env);
  const e2eCypressJob = createE2ECypressJob(res, env);
  const perfLintBuildJob = createPerfLintBuildJob(res, env);

  return [
    wfJob(dockerBackendJob, {
      context: jobContext, name: 'Build APIM Management API docker image', requires: ['Build backend'],
      'apim-project': components.managementApi.project,
      'docker-context': 'gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target',
      'docker-image-name': components.managementApi.image,
    }),
    wfJob(dockerBackendJob, {
      context: jobContext, name: 'Build APIM Gateway docker image', requires: ['Build backend'],
      'apim-project': components.gateway.project,
      'docker-context': 'gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-distribution/target',
      'docker-image-name': components.gateway.image,
    }),
    wfJob(e2eGenSdkJob, { context: jobContext, name: 'Generate e2e tests SDK', requires: ['Build backend'] }),
    wfJob(e2eLintBuildJob, { context: jobContext, name: 'Lint & Build APIM e2e', requires: ['Generate e2e tests SDK'] }),
    wfJob(perfLintBuildJob, { context: jobContext, name: 'Lint & Build APIM perf', requires: ['Generate e2e tests SDK'] }),
    wfJob(e2eTestJob, {
      context: jobContext,
      name: 'E2E - << matrix.execution_mode >> - << matrix.database >>',
      requires: ['Lint & Build APIM e2e', 'Build APIM Management API docker image', 'Build APIM Gateway docker image'],
      matrix: { execution_mode: ['v3', 'v4-emulation-engine'], database: ['mongo', 'jdbc', 'bridge'] },
    }),
    wfJob(e2eCypressJob, {
      context: jobContext, name: 'Run Cypress UI tests',
      requires: [
        'Lint & Build APIM e2e', 'Build APIM Management API docker image', 'Build APIM Gateway docker image',
        'Build APIM Console docker image', 'Build APIM Portal docker image',
      ],
    }),
  ];
}

// ─── Public API ──────────────────────────────────────────────────────────────

export function generatePullRequestsConfig(environment: CircleCIEnvironment): Config {
  const res: PipelineResources = { commands: new Map(), orbs: new Set() };
  const shouldBuildDockerImages = isSupportBranchOrMaster(environment.branch) || isE2EBranch(environment.branch);

  // Force isDryRun for helm publish in internal repo
  environment.isDryRun = true;

  let wfJobs: WorkflowJobEntry[];

  if (isSupportBranchOrMaster(environment.branch)) {
    wfJobs = [
      ...getCommonJobs(res, environment, false, false, shouldBuildDockerImages),
      ...getE2EJobs(res, environment),
      // Master & support-specific jobs would go here (publish, deploy, etc.)
    ];
  } else if (isE2EBranch(environment.branch)) {
    wfJobs = [
      ...getCommonJobs(res, environment, false, true, shouldBuildDockerImages),
      ...getE2EJobs(res, environment),
    ];
  } else {
    wfJobs = getCommonJobs(res, environment, true, true, shouldBuildDockerImages);
  }

  const prWorkflow = workflow('pull_requests', wfJobs);

  // Collect all unique jobs
  const jobMap = new Map<string, ReturnType<typeof job>>();
  for (const entry of wfJobs) {
    if (!('kind' in entry.jobRef)) {
      jobMap.set(entry.jobRef.name, entry.jobRef);
    }
  }

  return config({
    orbs: [...res.orbs],
    commands: [...res.commands.values()],
    jobs: [...jobMap.values()],
    workflows: [prWorkflow],
  });
}
