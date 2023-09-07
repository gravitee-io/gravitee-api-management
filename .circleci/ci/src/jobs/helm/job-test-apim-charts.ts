import { commands, Config, Job, reusable } from '@circleci/circleci-config-sdk';
import { Command } from '@circleci/circleci-config-sdk/dist/src/lib/Components/Commands/exports/Command';
import { orbs } from '../../orbs';
import { BaseExecutor } from '../../executors';

export class TestApimChartsJob {
  private static jobName = 'job-test-apim-charts';

  public static create(dynamicConfig: Config): Job {
    dynamicConfig.importOrb(orbs.helm);

    const steps: Command[] = [
      new commands.Checkout(),
      new reusable.ReusedCommand(orbs.helm.commands['install-helm-client'], { version: 'v3.7.1' }),
      new commands.Run({
        name: 'Install helm-unittest plugin',
        command: `helm plugin install https://github.com/quintush/helm-unittest --version 0.2.11`,
      }),
      new commands.Run({
        name: 'Lint the helm charts available in helm/',
        command: `helm lint helm/`,
      }),
      new commands.Run({
        name: 'Execute the units tests in helm/',
        command: "helm unittest -3 -f 'tests/**/*.yaml' helm/ -t JUnit -o apim-result.xml",
      }),
      new commands.StoreTestResults({
        path: 'apim-result.xml',
      }),
    ];
    return new Job(TestApimChartsJob.jobName, BaseExecutor.create('small'), steps);
  }
}
