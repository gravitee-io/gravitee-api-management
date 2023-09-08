import { commands, Config, Job, parameters, reusable } from '@circleci/circleci-config-sdk';
import { SonarScannerExecutor } from '../executors';
import { NotifyOnFailureCommand, RestoreMavenJobCacheCommand, SaveMavenJobCacheCommand } from '../commands';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { config } from '../config';
import { orbs } from '../orbs';
import { computeApimVersion } from '../utils';
import { CircleCIEnvironment } from '../pipelines';

export class SonarCloudAnalysisJob {
  private static jobName = 'job-sonarcloud-analysis';

  private static customParametersList = new parameters.CustomParametersList([
    new parameters.CustomParameter(
      'working_directory',
      'string',
      'gravitee-apim-rest-api',
      'Directory where the Sonarcloud analysis will be run',
    ),
  ]);

  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    dynamicConfig.importOrb(orbs.keeper);

    const apimVersion = computeApimVersion(environment);

    const restoreMavenJobCacheCmd = RestoreMavenJobCacheCommand.get();
    const saveMavenJobCacheCmd = SaveMavenJobCacheCommand.get();
    const notifyOnFailureCmd = NotifyOnFailureCommand.get(dynamicConfig);
    dynamicConfig.addReusableCommand(restoreMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(saveMavenJobCacheCmd);
    dynamicConfig.addReusableCommand(notifyOnFailureCmd);

    const steps: Command[] = [
      new commands.Run({
        name: 'Add SSH tool',
        command: 'apk add --no-cache openssh',
      }),
      new commands.Checkout(),
      new commands.workspace.Attach({ at: '.' }),
      new commands.cache.Restore({
        keys: [
          `${config.cache.prefix}-sonarcloud-analysis-<< parameters.working_directory >>-{{ .Branch }}-{{ checksum "pom.xml" }}`,
          `${config.cache.prefix}-sonarcloud-analysis-<< parameters.working_directory >>-{{ .Branch }}`,
          `${config.cache.prefix}-sonarcloud-analysis-<< parameters.working_directory >>`,
        ],
      }),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.sonarToken,
        'var-name': 'SONAR_TOKEN',
      }),
      new commands.Run({
        name: 'Run Sonarcloud Analysis',
        command: `sonar-scanner -Dsonar.projectVersion=${apimVersion}`,
        working_directory: '<< parameters.working_directory >>',
      }),
      new reusable.ReusedCommand(notifyOnFailureCmd),
      new commands.cache.Save({
        paths: ['/opt/sonar-scanner/.sonar/cache'],
        key: `${config.cache.prefix}-sonarcloud-analysis-<< parameters.working_directory >>-{{ .Branch }}-{{ checksum "pom.xml" }}`,
        when: 'always',
      }),
    ];

    return new reusable.ParameterizedJob(
      SonarCloudAnalysisJob.jobName,
      SonarScannerExecutor.create('large'),
      SonarCloudAnalysisJob.customParametersList,
      steps,
    );
  }
}
