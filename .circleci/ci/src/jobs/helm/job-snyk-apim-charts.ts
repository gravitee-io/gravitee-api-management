import { commands, Config, Job, reusable } from '@circleci/circleci-config-sdk';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { orbs } from '../../orbs';
import { config } from '../../config';
import { CircleCIEnvironment } from '../../pipelines';
import { BaseExecutor } from '../../executors';

export class SnykApimChartsJob {
  private static jobName = 'job-snyk-apim-charts';
  public static create(dynamicConfig: Config, environment: CircleCIEnvironment): Job {
    dynamicConfig.importOrb(orbs.helm);
    dynamicConfig.importOrb(orbs.keeper);
    dynamicConfig.importOrb(orbs.snyk);

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(orbs.keeper.commands['env-export'], {
        'secret-url': config.secrets.snykApiToken,
        'var-name': 'SNYK_TOKEN',
      }),
      new reusable.ReusedCommand(orbs.helm.commands['install-helm-client']),
      new reusable.ReusedCommand(orbs.snyk.commands['install']),
      new commands.Run({
        name: 'Build the Charts ouput and scan',
        command: `helm dependency update
helm template . --output-dir ./output
snyk iac test ./output --report --target-reference="${environment.branch}" --project-tags=version=${environment.branch} --severity-threshold=high`,
        working_directory: './helm',
      }),
    ];
    return new Job(SnykApimChartsJob.jobName, BaseExecutor.create('small'), steps);
  }
}
