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
 * The suites whose coverage report an analysis reads, and the project each one feeds.
 *
 * Single source of truth for the pull-request workflow and for the nightly build: a module
 * added here is analysed on a pull request and on the reference branch, or on neither. The
 * `predicate` narrows the pull-request scope to what the change could have touched; the
 * nightly ignores it and takes them all.
 */
export interface AnalysedProject {
  /** Workflow job name of the suite that produces the coverage report. */
  suiteName: string;
  /** Sonar project directory, holding its `sonar-project.properties`. */
  sonarProject: string;
  createSuite: (dynamicConfig: Config, environment: CircleCIEnvironment) => Job;
  predicate: (changedFiles: string[]) => boolean;
  /** Which scanner cache the analysis restores. Fixed by the list a project belongs to. */
  cacheType: 'backend' | 'frontend';
  /** Parameters the suite job takes, beyond its name and context. */
  suiteParameters?: Record<string, string>;
}

export const BACKEND_ANALYSED_PROJECTS: AnalysedProject[] = [
  {
    suiteName: 'Test definition',
    sonarProject: 'gravitee-apim-definition',
    createSuite: TestDefinitionJob.create,
    predicate: shouldTestDefinition,
    cacheType: 'backend',
  },
  {
    suiteName: 'Test gateway',
    sonarProject: 'gravitee-apim-gateway',
    createSuite: TestGatewayJob.create,
    predicate: shouldTestGateway,
    cacheType: 'backend',
  },
  {
    suiteName: 'Test rest-api',
    sonarProject: 'gravitee-apim-rest-api',
    createSuite: TestRestApiJob.create,
    predicate: shouldTestRestApi,
    cacheType: 'backend',
  },
  {
    suiteName: 'Test plugins',
    sonarProject: 'gravitee-apim-plugin',
    createSuite: TestPluginJob.create,
    predicate: shouldTestPlugin,
    cacheType: 'backend',
  },
  {
    suiteName: 'Test reporters',
    sonarProject: 'gravitee-apim-reporter',
    createSuite: TestReporterJob.create,
    predicate: shouldTestReporter,
    cacheType: 'backend',
  },
  {
    suiteName: 'Test repository',
    sonarProject: 'gravitee-apim-repository',
    createSuite: TestRepositoryJob.create,
    predicate: shouldTestRepository,
    cacheType: 'backend',
  },
];

/**
 * The backend suites no analysis reads, listed here for the same reason as the ones above: both
 * workflows take them from one place. They are the ones the coverage aggregation module does not
 * cover, so nothing else would have pinned them together — which is how the kafka-explorer and
 * gamma suites once stopped running at night when they were split into jobs of their own.
 */
export interface UnanalysedSuite {
  suiteName: string;
  createSuite: (dynamicConfig: Config, environment: CircleCIEnvironment) => Job;
  predicate: (changedFiles: string[]) => boolean;
  /** False for a suite that reads nothing the Maven build produces. Defaults to true. */
  requiresBuildBackend?: boolean;
}

export const BACKEND_UNANALYSED_SUITES: UnanalysedSuite[] = [
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

export const FRONTEND_ANALYSED_PROJECTS: AnalysedProject[] = [
  {
    suiteName: 'Lint & test APIM Console',
    sonarProject: config.components.console.project,
    createSuite: WebuiLintTestJob.createNx,
    predicate: shouldBuildConsole,
    cacheType: 'frontend',
    suiteParameters: {
      'apim-ui-project-workdir': config.components.console.workdir,
      'nx-project': 'console',
      resource_class: 'xlarge',
      'max-workers': '7',
    },
  },
  {
    suiteName: 'Lint & test APIM Portal Next',
    sonarProject: config.components.portal.next.project,
    createSuite: WebuiLintTestJob.createNx,
    predicate: shouldBuildPortalNext,
    cacheType: 'frontend',
    suiteParameters: {
      'apim-ui-project-workdir': config.components.portal.next.project,
      'nx-project': 'portal-next',
      'max-workers': '2',
    },
  },
  {
    suiteName: 'Lint & test APIM Portal',
    sonarProject: config.components.portal.workdir,
    createSuite: WebuiLintTestJob.create,
    predicate: shouldBuildPortal,
    cacheType: 'frontend',
    suiteParameters: {
      'apim-ui-project': config.components.portal.project,
      'apim-ui-project-workdir': config.components.portal.workdir,
      resource_class: 'large',
    },
  },
];

/** The analysis job that reads the report a suite left behind. Its only dependency is that suite. */
export function analysisJobFor(project: AnalysedProject, sonarAnalysisJob: Job): workflow.WorkflowJob {
  return new workflow.WorkflowJob(sonarAnalysisJob, {
    name: `Sonar - ${project.sonarProject}`,
    context: config.jobContext,
    requires: [project.suiteName],
    working_directory: project.sonarProject,
    cache_type: project.cacheType,
  });
}
