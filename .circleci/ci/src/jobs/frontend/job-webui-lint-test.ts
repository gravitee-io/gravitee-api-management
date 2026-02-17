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
import { commands, Config, Job, parameters, reusable } from '@circleci/circleci-config-sdk';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { CommandParameterLiteral } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Parameters/types/CustomParameterLiterals.types';
import { NodeLtsExecutor } from '../../executors';
import { InstallYarnCommand, NotifyOnFailureCommand, WebuiInstallCommand, WorkspaceInstallCommand } from '../../commands';
import { CircleCIEnvironment } from '../../pipelines';

export class WebuiLintTestJob {
  private static jobName = 'job-webui-lint-test';
  private static jobNameNx = 'job-nx-webui-lint-test';

  private static customParametersList = new parameters.CustomParametersList<CommandParameterLiteral>([
    new parameters.CustomParameter('apim-ui-project', 'string', '', 'the directory name of the UI project'),
    new parameters.CustomParameter('resource_class', 'string', 'medium', 'Resource class to use for executor'),
  ]);

  private static customParametersListNx = new parameters.CustomParametersList<CommandParameterLiteral>([
    new parameters.CustomParameter('apim-ui-project', 'string', '', 'the directory name of the UI project'),
    new parameters.CustomParameter('nx-project', 'string', '', 'the Nx project name'),
    new parameters.CustomParameter('resource_class', 'string', 'medium', 'Resource class to use for executor'),
    new parameters.CustomParameter('max-workers', 'string', '35%', 'Maximum number of workers for Jest tests'),
  ]);

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const installYarnCmd = InstallYarnCommand.get();
    dynamicConfig.addReusableCommand(installYarnCmd);

    const webUiInstallCommand = WebuiInstallCommand.get();
    dynamicConfig.addReusableCommand(webUiInstallCommand);

    const notifyOnFailureCommand = NotifyOnFailureCommand.get(dynamicConfig, environment);
    dynamicConfig.addReusableCommand(notifyOnFailureCommand);

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(installYarnCmd),
      new reusable.ReusedCommand(webUiInstallCommand, { 'apim-ui-project': '<< parameters.apim-ui-project >>' }),
      new commands.workspace.Attach({ at: '.' }),
      new commands.Run({
        name: 'Check License',
        command: 'yarn lint:license',
        working_directory: '<< parameters.apim-ui-project >>',
      }),
      new commands.Run({
        name: 'Run Prettier and ESLint',
        command: 'yarn lint',
        working_directory: '<< parameters.apim-ui-project >>',
      }),
      new commands.Run({
        name: 'Run unit tests',
        command: 'yarn test:coverage',
        working_directory: '<< parameters.apim-ui-project >>',
      }),
      new reusable.ReusedCommand(notifyOnFailureCommand),
      // For Sonar analysis
      new commands.workspace.Persist({
        root: '.',
        paths: ['<< parameters.apim-ui-project >>/coverage/lcov.info'],
      }),
      // For direct access in CircleCI UI
      new commands.StoreArtifacts({
        path: '<< parameters.apim-ui-project >>/coverage/lcov.info',
      }),
      // For Test tab in CircleCI UI
      new commands.StoreTestResults({
        path: '<< parameters.apim-ui-project >>/coverage/junit.xml',
      }),
    ];

    return new reusable.ParameterizedJob(
      WebuiLintTestJob.jobName,
      NodeLtsExecutor.create('<< parameters.resource_class >>'),
      WebuiLintTestJob.customParametersList,
      steps,
    );
  }

  public static createNx(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const installYarnCmd = InstallYarnCommand.get();
    dynamicConfig.addReusableCommand(installYarnCmd);

    const workspaceInstallCommand = WorkspaceInstallCommand.get();
    dynamicConfig.addReusableCommand(workspaceInstallCommand);

    const notifyOnFailureCommand = NotifyOnFailureCommand.get(dynamicConfig, environment);
    dynamicConfig.addReusableCommand(notifyOnFailureCommand);

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(installYarnCmd),
      new reusable.ReusedCommand(workspaceInstallCommand),
      new commands.workspace.Attach({ at: '.' }),
      new commands.Run({
        name: 'Check License',
        command: 'yarn nx run << parameters.nx-project >>:lint-license',
      }),
      new commands.Run({
        name: 'Run Prettier and ESLint',
        command: 'yarn nx lint << parameters.nx-project >>',
      }),
      new commands.Run({
        name: 'Run unit tests',
        command: 'yarn nx test << parameters.nx-project >> --coverage --maxWorkers=<< parameters.max-workers >>',
      }),
      new reusable.ReusedCommand(notifyOnFailureCommand),
      // For Sonar analysis
      new commands.workspace.Persist({
        root: '.',
        paths: ['<< parameters.apim-ui-project >>/coverage/lcov.info'],
      }),
      // For direct access in CircleCI UI
      new commands.StoreArtifacts({
        path: '<< parameters.apim-ui-project >>/coverage/lcov.info',
      }),
      // For Test tab in CircleCI UI
      new commands.StoreTestResults({
        path: '<< parameters.apim-ui-project >>/coverage/junit.xml',
      }),
    ];

    return new reusable.ParameterizedJob(
      WebuiLintTestJob.jobNameNx,
      NodeLtsExecutor.create('<< parameters.resource_class >>'),
      WebuiLintTestJob.customParametersListNx,
      steps,
    );
  }

  public static createNxLibs(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    const installYarnCmd = InstallYarnCommand.get();
    dynamicConfig.addReusableCommand(installYarnCmd);

    const workspaceInstallCommand = WorkspaceInstallCommand.get();
    dynamicConfig.addReusableCommand(workspaceInstallCommand);

    const notifyOnFailureCommand = NotifyOnFailureCommand.get(dynamicConfig, environment);
    dynamicConfig.addReusableCommand(notifyOnFailureCommand);

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(installYarnCmd),
      new reusable.ReusedCommand(workspaceInstallCommand),
      new commands.workspace.Attach({ at: '.' }),
      new commands.Run({
        name: 'Lint APIM Libs (affected)',
        command: 'yarn nx affected -t lint --exclude=console,portal-next',
      }),
      new commands.Run({
        name: 'Test APIM Libs (affected)',
        command: 'yarn nx affected -t test --exclude=console,portal-next --coverage --maxWorkers=2',
      }),
      new reusable.ReusedCommand(notifyOnFailureCommand),
      // Ensure coverage files exist so persist/store steps don't fail when libs are not affected
      new commands.Run({
        name: 'Ensure coverage files exist',
        command: [
          'mkdir -p gravitee-apim-webui-libs/gravitee-markdown/coverage',
          'mkdir -p gravitee-apim-webui-libs/gravitee-dashboard/coverage',
          'touch gravitee-apim-webui-libs/gravitee-markdown/coverage/lcov.info',
          'touch gravitee-apim-webui-libs/gravitee-dashboard/coverage/lcov.info',
        ].join('\n'),
      }),
      // Store artifacts and test results for markdown
      new commands.workspace.Persist({
        root: '.',
        paths: ['gravitee-apim-webui-libs/gravitee-markdown/coverage/lcov.info'],
      }),
      new commands.StoreArtifacts({
        path: 'gravitee-apim-webui-libs/gravitee-markdown/coverage/lcov.info',
      }),
      new commands.StoreTestResults({
        path: 'gravitee-apim-webui-libs/gravitee-markdown/coverage/junit.xml',
      }),
      // Store artifacts and test results for dashboard
      new commands.workspace.Persist({
        root: '.',
        paths: ['gravitee-apim-webui-libs/gravitee-dashboard/coverage/lcov.info'],
      }),
      new commands.StoreArtifacts({
        path: 'gravitee-apim-webui-libs/gravitee-dashboard/coverage/lcov.info',
      }),
      new commands.StoreTestResults({
        path: 'gravitee-apim-webui-libs/gravitee-dashboard/coverage/junit.xml',
      }),
    ];

    return new Job('job-webui-libs-lint-test', NodeLtsExecutor.create('medium'), steps);
  }
}
