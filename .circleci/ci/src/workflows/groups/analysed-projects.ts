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
import { Config, Job } from '../../circleci-config';
import {
  TestDefinitionJob,
  TestGatewayJob,
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
  shouldTestGateway,
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
  /** Parameters the suite job takes, beyond its name and context. */
  suiteParameters?: Record<string, string>;
}

export const BACKEND_ANALYSED_PROJECTS: AnalysedProject[] = [
  {
    suiteName: 'Test definition',
    sonarProject: 'gravitee-apim-definition',
    createSuite: TestDefinitionJob.create,
    predicate: shouldTestDefinition,
  },
  {
    suiteName: 'Test gateway',
    sonarProject: 'gravitee-apim-gateway',
    createSuite: TestGatewayJob.create,
    predicate: shouldTestGateway,
  },
  {
    suiteName: 'Test rest-api',
    sonarProject: 'gravitee-apim-rest-api',
    createSuite: TestRestApiJob.create,
    predicate: shouldTestRestApi,
  },
  {
    suiteName: 'Test plugins',
    sonarProject: 'gravitee-apim-plugin',
    createSuite: TestPluginJob.create,
    predicate: shouldTestPlugin,
  },
  {
    suiteName: 'Test reporters',
    sonarProject: 'gravitee-apim-reporter',
    createSuite: TestReporterJob.create,
    predicate: shouldTestReporter,
  },
  {
    suiteName: 'Test repository',
    sonarProject: 'gravitee-apim-repository',
    createSuite: TestRepositoryJob.create,
    predicate: shouldTestRepository,
  },
];

export const FRONTEND_ANALYSED_PROJECTS: AnalysedProject[] = [
  {
    suiteName: 'Lint & test APIM Console',
    sonarProject: config.components.console.project,
    createSuite: WebuiLintTestJob.createNx,
    predicate: shouldBuildConsole,
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
    suiteParameters: {
      'apim-ui-project': config.components.portal.project,
      'apim-ui-project-workdir': config.components.portal.workdir,
      resource_class: 'large',
    },
  },
];

/** The analysis job that reads the report a suite left behind. Its only dependency is that suite. */
export function analysisJobFor(
  project: AnalysedProject,
  sonarAnalysisJob: Job,
  cacheType: 'backend' | 'frontend',
): { job: Job; parameters: Record<string, string | string[]> } {
  return {
    job: sonarAnalysisJob,
    parameters: {
      name: `Sonar - ${project.sonarProject}`,
      context: config.jobContext,
      requires: [project.suiteName],
      working_directory: project.sonarProject,
      cache_type: cacheType,
    },
  };
}
