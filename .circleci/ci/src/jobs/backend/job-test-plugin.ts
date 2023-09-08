import { commands, Config } from '@circleci/circleci-config-sdk';
import { config } from '../../config';
import { AbstractTestJob } from './abstract-job-test';
import { UbuntuExecutor } from '../../executors';

export class TestPluginJob extends AbstractTestJob {
  public static create(dynamicConfig: Config) {
    return super.create(
      dynamicConfig,
      'job-test-plugin',
      new commands.Run({
        name: `Run plugin tests`,
        command: `mvn --fail-fast -s ${config.maven.settingsFile} test --no-transfer-progress -Dplugin-modules -Dskip.validation=true -T 2C`,
      }),
      UbuntuExecutor.create(),
      ['gravitee-apim-plugin/gravitee-apim-plugin-coverage/target/site/jacoco-aggregate/'],
    );
  }
}
