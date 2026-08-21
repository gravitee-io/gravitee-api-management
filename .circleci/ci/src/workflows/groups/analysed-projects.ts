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
import { Config, Job, workflow } from '../../circleci-config';
import {
  TestDefinitionJob,
  TestGammaJob,
  TestGammaUiJob,
  TestGatewayJob,
  TestIntegrationJob,
  TestKafkaExplorerJob,
  TestPluginJob,
  TestReporterJob,
  TestRepositoryJob,
  TestRestApiJob,
  WebuiLintTestJob,
} from '../../jobs';
import { CircleCIEnvironment } from '../../pipelines';
import { config } from '../../config';
import {
  shouldBuildConsole,
  shouldBuildPortal,
  shouldBuildPortalNext,
  shouldTestDefinition,
  shouldTestGamma,
  shouldTestGateway,
  shouldTestIntegrationTests,
  shouldTestPlugin,
  shouldTestReporter,
  shouldTestRepository,
  shouldTestRestApi,
} from './changed-files';

/**
 * Every test suite a workflow can run, and the analysis that reads its coverage report when
 * there is one.
 *
 * Single source of truth for the pull-request workflow and for the nightly build: a suite added
 * here runs on a pull request that touches it and on the reference branch every night, or on
 * neither. The `predicate` narrows the pull-request scope; the nightly ignores it.
 *
 * `sonar` is an attribute, not a category: kafka-explorer, the two gamma suites and the
 * integration tests have no analysis because the coverage aggregation module does not list them,
 * and that is the only thing that sets them apart. Splitting them into a second list is what let
 * them fall out of the nightly unnoticed.
 */
export interface Suite {
  /** Workflow job name, and what a downstream gate waits on. */
  suiteName: string;
  createSuite: (dynamicConfig: Config, environment: CircleCIEnvironment) => Job;
  predicate: (changedFiles: string[]) => boolean;
  /** The analysis that reads this suite's coverage report. Absent when no analysis does. */
  sonar?: {
    /** Sonar project directory, holding its `sonar-project.properties`. */
    project: string;
    /** Which scanner cache the analysis restores. */
    cacheType: 'backend' | 'frontend';
  };
  /** Parameters the suite job takes, beyond its name and context. */
  suiteParameters?: Record<string, string>;
  /** False for a suite that reads nothing the Maven build produces. Defaults to true. */
  requiresBuildBackend?: boolean;
}

export const BACKEND_SUITES: Suite[] = [
  {
    suiteName: 'Test definition',
    createSuite: TestDefinitionJob.create,
    predicate: shouldTestDefinition,
    sonar: { project: 'gravitee-apim-definition', cacheType: 'backend' },
  },
  {
    suiteName: 'Test gateway',
    createSuite: TestGatewayJob.create,
    predicate: shouldTestGateway,
    sonar: { project: 'gravitee-apim-gateway', cacheType: 'backend' },
  },
  {
    suiteName: 'Test rest-api',
    createSuite: TestRestApiJob.create,
    predicate: shouldTestRestApi,
    sonar: { project: 'gravitee-apim-rest-api', cacheType: 'backend' },
  },
  {
    suiteName: 'Test plugins',
    createSuite: TestPluginJob.create,
    predicate: shouldTestPlugin,
    sonar: { project: 'gravitee-apim-plugin', cacheType: 'backend' },
  },
  {
    suiteName: 'Test reporters',
    createSuite: TestReporterJob.create,
    predicate: shouldTestReporter,
    sonar: { project: 'gravitee-apim-reporter', cacheType: 'backend' },
  },
  {
    suiteName: 'Test repository',
    createSuite: TestRepositoryJob.create,
    predicate: shouldTestRepository,
    sonar: { project: 'gravitee-apim-repository', cacheType: 'backend' },
  },

  // The only containerised module, kept out of the rest-api reactor; it shares that reactor's
  // predicate.
  { suiteName: 'Test kafka-explorer', createSuite: TestKafkaExplorerJob.create, predicate: shouldTestRestApi },
  { suiteName: 'Test gamma', createSuite: TestGammaJob.create, predicate: shouldTestGamma },
  // Nothing this job reads comes from the Maven build: it checks out, installs the yarn workspace
  // and runs nx. Gating it on Build backend would serialise a pure-JS job behind the whole
  // reactor, which the other front-end jobs are careful not to do.
  {
    suiteName: 'Test gamma UI',
    createSuite: TestGammaUiJob.create,
    predicate: shouldTestGamma,
    requiresBuildBackend: false,
  },
  { suiteName: 'Integration tests', createSuite: TestIntegrationJob.create, predicate: shouldTestIntegrationTests },
];

export const FRONTEND_SUITES: Suite[] = [
  {
    suiteName: 'Lint & test APIM Console',
    createSuite: WebuiLintTestJob.createNx,
    requiresBuildBackend: false,
    predicate: shouldBuildConsole,
    sonar: { project: config.components.console.project, cacheType: 'frontend' },
    suiteParameters: {
      'apim-ui-project-workdir': config.components.console.workdir,
      'nx-project': 'console',
      resource_class: 'xlarge',
      'max-workers': '7',
    },
  },
  {
    suiteName: 'Lint & test APIM Portal Next',
    createSuite: WebuiLintTestJob.createNx,
    requiresBuildBackend: false,
    predicate: shouldBuildPortalNext,
    sonar: { project: config.components.portal.next.project, cacheType: 'frontend' },
    suiteParameters: {
      'apim-ui-project-workdir': config.components.portal.next.project,
      'nx-project': 'portal-next',
      'max-workers': '2',
    },
  },
  {
    suiteName: 'Lint & test APIM Portal',
    createSuite: WebuiLintTestJob.create,
    requiresBuildBackend: false,
    predicate: shouldBuildPortal,
    sonar: { project: config.components.portal.workdir, cacheType: 'frontend' },
    suiteParameters: {
      'apim-ui-project': config.components.portal.project,
      'apim-ui-project-workdir': config.components.portal.workdir,
      resource_class: 'large',
    },
  },
];

/** Looks a suite up by the name it carries in the list. Throws rather than emitting a job the
 * workflow silently gets wrong. */
export function suiteNamed(name: string): Suite {
  const suite = [...BACKEND_SUITES, ...FRONTEND_SUITES].find((candidate) => candidate.suiteName === name);
  if (!suite) {
    throw new Error(`No suite named ${name}`);
  }
  return suite;
}

/** The suite job itself, with the parameters the list holds for it. */
export function suiteJobFor(suite: Suite, dynamicConfig: Config, environment: CircleCIEnvironment): workflow.WorkflowJob {
  const suiteJob = suite.createSuite(dynamicConfig, environment);
  dynamicConfig.addJob(suiteJob);
  return new workflow.WorkflowJob(suiteJob, {
    name: suite.suiteName,
    context: config.jobContext,
    ...(suite.requiresBuildBackend === false ? {} : { requires: ['Build backend'] }),
    ...(suite.suiteParameters ?? {}),
  });
}

/** The analysis job that reads the report a suite left behind. Its only dependency is that suite. */
export function analysisJobFor(suite: Suite, sonarAnalysisJob: Job): workflow.WorkflowJob {
  if (!suite.sonar) {
    throw new Error(`No analysis is declared for ${suite.suiteName}`);
  }
  return new workflow.WorkflowJob(sonarAnalysisJob, {
    name: `Sonar - ${suite.sonar.project}`,
    context: config.jobContext,
    requires: [suite.suiteName],
    working_directory: suite.sonar.project,
    cache_type: suite.sonar.cacheType,
  });
}
