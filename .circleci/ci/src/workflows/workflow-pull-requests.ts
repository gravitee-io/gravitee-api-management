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
import { commands, Config, Job, workflow, Workflow } from '../circleci-config';

import { CircleCIEnvironment } from '../pipelines';
import { isE2EBranch, isSupportBranchOrMaster } from '../utils';
import { config } from '../config';
import { BaseExecutor } from '../executors';
import { DangerJsJob, TestApimChartsJob } from '../jobs';
import { orbs } from '../orbs';
import { backendImageJobs } from './groups/backend-image-jobs';
import { e2eJobs } from './groups/e2e-jobs';
import { chainguardFipsJobs } from './groups/chainguard-fips-jobs';
import { masterAndSupportJobs } from './groups/master-and-support-jobs';
import { backendJobs } from './groups/backend-jobs';
import { frontendJobs } from './groups/frontend-jobs';
import { shouldBuildHelm } from './groups/changed-files';

export class PullRequestsWorkflow {
  static create(dynamicConfig: Config, environment: CircleCIEnvironment): Workflow {
    let jobs: workflow.WorkflowJob[] = [];
    const shouldBuildDockerImages: boolean = isSupportBranchOrMaster(environment.branch) || isE2EBranch(environment.branch);
    // Needed to publish helm chart in internal repository
    environment.isDryRun = true;
    if (isSupportBranchOrMaster(environment.branch)) {
      jobs.push(
        ...this.getCommonJobs(dynamicConfig, environment, false, false, shouldBuildDockerImages),
        ...backendImageJobs(dynamicConfig, environment),
        ...e2eJobs(dynamicConfig, environment),
        ...(shouldBuildDockerImages ? chainguardFipsJobs(dynamicConfig, environment) : []),
        ...masterAndSupportJobs(dynamicConfig, environment),
      );
    } else if (isE2EBranch(environment.branch)) {
      jobs.push(
        ...this.getCommonJobs(dynamicConfig, environment, false, true, shouldBuildDockerImages),
        ...backendImageJobs(dynamicConfig, environment),
        ...e2eJobs(dynamicConfig, environment),
      );
    } else {
      jobs = this.getCommonJobs(dynamicConfig, environment, true, true, shouldBuildDockerImages);
    }
    return new Workflow('pull_requests', jobs);
  }

  private static getCommonJobs(
    dynamicConfig: Config,
    environment: CircleCIEnvironment,
    filterJobs: boolean,
    addValidationJob: boolean,
    shouldBuildDockerImages: boolean,
  ): workflow.WorkflowJob[] {
    dynamicConfig.importOrb(orbs.keeper);

    const dangerJSJob = DangerJsJob.create(dynamicConfig);
    dynamicConfig.addJob(dangerJSJob);

    const jobs: workflow.WorkflowJob[] = [
      new workflow.WorkflowJob(dangerJSJob, {
        name: 'Run Danger JS',
        context: config.jobContext,
      }),
    ];
    const requires: string[] = [];

    if (!filterJobs || shouldBuildHelm(environment.changedFiles)) {
      const apimChartsTestJob = TestApimChartsJob.create(dynamicConfig, environment);
      dynamicConfig.addJob(apimChartsTestJob);
      jobs.push(
        new workflow.WorkflowJob(apimChartsTestJob, {
          name: 'Helm Chart - Lint & Test',
          context: config.jobContext,
        }),
      );

      requires.push('Helm Chart - Lint & Test');
    }

    const backend = backendJobs(dynamicConfig, environment, filterJobs);
    jobs.push(...backend.jobs);
    requires.push(...backend.requires);

    const frontend = frontendJobs(dynamicConfig, environment, filterJobs, shouldBuildDockerImages);
    jobs.push(...frontend.jobs);
    requires.push(...frontend.requires);

    // Force validation workflow in case only distribution pom.xml has changed
    if (!requires.includes('Build backend') && environment.changedFiles.some((file) => file.includes('gravitee-apim-distribution'))) {
      addValidationJob = true;
      requires.push('Build backend');
    }

    // compute check-workflow job
    if (addValidationJob && requires.length > 0) {
      const checkWorkflowJob = new Job('job-validate-workflow-status', BaseExecutor.create('small'), [
        new commands.Run({
          name: 'Check workflow jobs',
          command: 'echo "Congratulations! If you can read this, everything is OK"',
        }),
      ]);
      dynamicConfig.addJob(checkWorkflowJob);
      jobs.push(new workflow.WorkflowJob(checkWorkflowJob, { name: 'Validate workflow status', requires }));
    }

    return jobs;
  }

  // FIPS product images built on support-branch/master PRs, pushed to the private Azure
  // registry. Java components use the java-fips base; the UIs (nginx) use the nginx-fips base.
  // Relies on the 'Build backend' / 'Build APIM Console|Portal', 'Build Gamma Console' jobs
  // from getCommonJobs.
}
