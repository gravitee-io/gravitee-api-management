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
import { BuildBackendJob, CheckGrapheneVersionsJob, SetupJob, SonarCloudAnalysisJob, ValidateJob } from '../../jobs';
import { analysisJobFor, BACKEND_SUITES, suiteJobFor } from './analysed-projects';
import { CircleCIEnvironment } from '../../pipelines';
import { config } from '../../config';
import { shouldBuildBackend } from './changed-files';

/**
 * Setup, validation, the engine build and the per-module test suites.
 *
 * Returns both the jobs and the names a downstream gate should wait on: callers accumulate
 * the two lists in the order they contribute the groups, which is what keeps the emitted
 * job list and the `requires` list stable.
 */
export function backendJobs(
  dynamicConfig: Config,
  environment: CircleCIEnvironment,
  filterJobs: boolean,
): { jobs: workflow.WorkflowJob[]; requires: string[] } {
  const jobs: workflow.WorkflowJob[] = [];
  const requires: string[] = [];

  if (filterJobs && !shouldBuildBackend(environment.changedFiles)) {
    return { jobs, requires };
  }

  const setupJob = SetupJob.create(dynamicConfig);
  dynamicConfig.addJob(setupJob);

  const validateBackendJob = ValidateJob.create(dynamicConfig, environment);
  dynamicConfig.addJob(validateBackendJob);

  const buildBackendJob = BuildBackendJob.create(dynamicConfig, environment);
  dynamicConfig.addJob(buildBackendJob);

  const checkGrapheneVersionsJob = CheckGrapheneVersionsJob.create();
  dynamicConfig.addJob(checkGrapheneVersionsJob);

  jobs.push(
    new workflow.WorkflowJob(setupJob, { name: 'Setup', context: config.jobContext }),
    new workflow.WorkflowJob(validateBackendJob, {
      name: 'Validate backend',
      context: config.jobContext,
      requires: ['Setup'],
    }),
    new workflow.WorkflowJob(buildBackendJob, {
      name: 'Build backend',
      context: config.jobContext,
      requires: ['Validate backend'],
    }),
    new workflow.WorkflowJob(checkGrapheneVersionsJob, {
      name: 'Check graphene versions',
      context: config.jobContext,
      requires: ['Build backend'],
    }),
  );
  requires.push('Build backend', 'Check graphene versions');

  // Created on the first project that survives the predicate: a pipeline that analyses nothing
  // must not emit an analysis job definition no workflow references.
  let sonarAnalysisJob: Job | undefined;

  BACKEND_SUITES.forEach((suite) => {
    if (filterJobs && !suite.predicate(environment.changedFiles)) {
      return;
    }

    jobs.push(suiteJobFor(suite, dynamicConfig, environment));

    if (suite.sonar) {
      if (!sonarAnalysisJob) {
        sonarAnalysisJob = SonarCloudAnalysisJob.create(dynamicConfig, environment);
        dynamicConfig.addJob(sonarAnalysisJob);
      }
      jobs.push(analysisJobFor(suite, sonarAnalysisJob));
    }

    requires.push(suite.suiteName);
  });

  return { jobs, requires };
}
