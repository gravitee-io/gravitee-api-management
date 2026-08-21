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
import { Command, commands, Config, Job, reusable } from '../../circleci-config';
import { InstallYarnCommand, NotifyOnFailureCommand, WorkspaceInstallCommand } from '../../commands';
import { NodeLtsExecutor } from '../../executors';
import { CircleCIEnvironment } from '../../pipelines';

// The Gamma UI modules ship inside their plugin zip and are built by Maven, so their Jest suites had no
// job of their own and never ran. Their Maven counterpart deliberately skips the Nx build, which is why
// the two halves are tested here rather than there.
export class TestGammaUiJob {
  private static jobName = 'job-test-gamma-ui';
  private static projects = ['gravitee-gamma-module-apim', 'gravitee-gamma-module-platform'];

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const installYarnCmd = InstallYarnCommand.get();
    dynamicConfig.addReusableCommand(installYarnCmd);

    const workspaceInstallCmd = WorkspaceInstallCommand.get();
    dynamicConfig.addReusableCommand(workspaceInstallCmd);

    const notifyOnFailureCmd = NotifyOnFailureCommand.get(dynamicConfig, environment);
    dynamicConfig.addReusableCommand(notifyOnFailureCmd);

    const projects = TestGammaUiJob.projects.join(',');

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(installYarnCmd),
      new reusable.ReusedCommand(workspaceInstallCmd),
      new commands.Run({
        // The lint target fans out to lint-eslint, lint-prettier and lint-license (nx.json).
        name: 'Run Prettier, ESLint and license check',
        command: `yarn nx run-many --target=lint --projects=${projects}`,
      }),
      new commands.Run({
        // Jest sizes its worker pool from os.cpus(), which inside a container reports the host's cores
        // rather than the resource class — the same trap mavenParallelism exists to avoid for -T. Left
        // alone it opens dozens of jsdom environments on a four-core executor.
        name: 'Run unit tests',
        command: `yarn nx run-many --target=test --projects=${projects} --maxWorkers=2 --coverage`,
      }),
      new reusable.ReusedCommand(notifyOnFailureCmd),
      ...TestGammaUiJob.projects.flatMap((project) => [
        new commands.StoreTestResults({ path: `gravitee-gamma/${project}/coverage/junit.xml` }),
        new commands.StoreArtifacts({ path: `gravitee-gamma/${project}/coverage/lcov.info` }),
      ]),
    ];

    return new Job(TestGammaUiJob.jobName, NodeLtsExecutor.create('large'), steps);
  }
}
