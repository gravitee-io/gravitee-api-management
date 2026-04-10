/**
 * Reusable commands for Gravitee APIM CI.
 */
import { reusableCommand, use, type ReusableCommandDefinition } from '../sdk/reusable.js';
import { run, cache, checkout, workspace, type CommandStep } from '../sdk/commands.js';
import { useOrb } from '../sdk/orbs.js';
import { param } from '../sdk/parameters.js';
import { keeper, slack } from '../orbs/index.js';
import { cachePrefix, maven, yarnVersion, secrets, slackChannels } from '../config/config.js';
import type { CircleCIEnvironment } from '../config/environment.js';

// ─── Maven cache ─────────────────────────────────────────────────────────────

export function restoreMavenCacheCommand(environment: CircleCIEnvironment): ReusableCommandDefinition {
  const keys = [
    `${cachePrefix}-<< parameters.jobName >>-{{ .Branch }}-{{ checksum "pom.xml" }}`,
    `${cachePrefix}-<< parameters.jobName >>-{{ .Branch }}`,
  ];
  if (environment.baseBranch !== environment.branch) {
    keys.push(`${cachePrefix}-<< parameters.jobName >>-${environment.baseBranch}`);
  }
  return reusableCommand(
    'cmd-restore-maven-job-cache',
    [cache.restore({ keys })],
    {
      parameters: { jobName: param.string('', 'The job name') },
      description: 'Restore Maven cache for a dedicated job',
    },
  );
}

export function saveMavenCacheCommand(): ReusableCommandDefinition {
  return reusableCommand(
    'cmd-save-maven-job-cache',
    [
      run('Exclude all APIM artefacts from cache.', 'rm -rf ~/.m2/repository/io/gravitee/apim'),
      cache.save({
        key: `${cachePrefix}-<< parameters.jobName >>-{{ .Branch }}-{{ checksum "pom.xml" }}`,
        paths: ['~/.m2'],
        when: 'always',
      }),
    ],
    {
      parameters: { jobName: param.string('', 'The job name') },
      description: 'Save Maven cache for a dedicated job',
    },
  );
}

// ─── Notify on failure ───────────────────────────────────────────────────────

export function notifyOnFailureCommand(): ReusableCommandDefinition {
  return reusableCommand('cmd-notify-on-failure', [
    useOrb(keeper, 'env-export', {
      'secret-url': secrets.slackAccessToken,
      'var-name': 'SLACK_ACCESS_TOKEN',
    }),
    useOrb(slack, 'notify', {
      channel: slackChannels.apiManagementTeamNotifications,
      branch_pattern: 'master,[0-9]+\\\\.[0-9]+\\\\.x',
      event: 'fail',
      template: 'basic_fail_1',
    }),
  ]);
}

// ─── Yarn install ────────────────────────────────────────────────────────────

export function installYarnCommand(): ReusableCommandDefinition {
  return reusableCommand('cmd-install-yarn', [
    run('Enable Corepack', 'corepack enable || sudo corepack enable'),
    run('Set Yarn version', `yarn set version ${yarnVersion}`),
  ]);
}

// ─── WebUI install (with yarn cache) ─────────────────────────────────────────

export function webuiInstallCommand(): ReusableCommandDefinition {
  return reusableCommand(
    'cmd-webui-install',
    [
      cache.restore({ keys: [`${cachePrefix}-webui-<< parameters.apim-ui-project >>-{{ checksum "<< parameters.apim-ui-project >>/yarn.lock" }}`] }),
      run('Install', {
        command: 'yarn install --immutable',
        workingDirectory: '<< parameters.apim-ui-project >>',
      }),
      cache.save({
        key: `${cachePrefix}-webui-<< parameters.apim-ui-project >>-{{ checksum "<< parameters.apim-ui-project >>/yarn.lock" }}`,
        paths: ['<< parameters.apim-ui-project >>/.yarn/cache'],
        when: 'always',
      }),
    ],
    { parameters: { 'apim-ui-project': param.string('', 'WebUI project directory') } },
  );
}

// ─── Workspace install (root-level yarn) ─────────────────────────────────────

export function workspaceInstallCommand(): ReusableCommandDefinition {
  return reusableCommand(
    'cmd-workspace-install',
    [
      cache.restore({ keys: [`${cachePrefix}-workspace-{{ checksum "yarn.lock" }}`] }),
      run('Install', 'yarn install --immutable'),
      cache.save({
        key: `${cachePrefix}-workspace-{{ checksum "yarn.lock" }}`,
        paths: ['.yarn/cache'],
        when: 'always',
      }),
    ],
  );
}

// ─── Docker commands ─────────────────────────────────────────────────────────

export function createDockerContextCommand(): ReusableCommandDefinition {
  return reusableCommand('cmd-create-docker-context', [
    run('Create Docker Buildx context', [
      'docker context create tls-env',
      'docker buildx create tls-env --use',
    ].join('\n')),
  ]);
}

export function dockerLoginCommand(environment: CircleCIEnvironment, isProd: boolean): ReusableCommandDefinition {
  const steps: CommandStep[] = [
    useOrb(keeper, 'env-export', {
      'secret-url': secrets.azureRegistryUsername,
      'var-name': 'AZURE_REGISTRY_USERNAME',
    }),
    useOrb(keeper, 'env-export', {
      'secret-url': secrets.azureRegistryPassword,
      'var-name': 'AZURE_REGISTRY_PASSWORD',
    }),
    run('Docker login (Azure)', 'echo "$AZURE_REGISTRY_PASSWORD" | docker login graviteeio.azurecr.io -u "$AZURE_REGISTRY_USERNAME" --password-stdin'),
  ];

  if (isProd) {
    steps.push(
      useOrb(keeper, 'env-export', {
        'secret-url': secrets.dockerhubBotUserName,
        'var-name': 'DOCKERHUB_BOT_USER_NAME',
      }),
      useOrb(keeper, 'env-export', {
        'secret-url': secrets.dockerhubBotUserToken,
        'var-name': 'DOCKERHUB_BOT_USER_TOKEN',
      }),
      run('Docker login (Docker Hub)', 'echo "$DOCKERHUB_BOT_USER_TOKEN" | docker login -u "$DOCKERHUB_BOT_USER_NAME" --password-stdin'),
    );
  }

  return reusableCommand('cmd-docker-login', steps);
}

export function dockerLogoutCommand(environment: CircleCIEnvironment, isProd: boolean): ReusableCommandDefinition {
  const steps: CommandStep[] = [
    run('Docker logout (Azure)', 'docker logout graviteeio.azurecr.io'),
  ];
  if (isProd) {
    steps.push(run('Docker logout (Docker Hub)', 'docker logout'));
  }
  return reusableCommand('cmd-docker-logout', steps);
}
