import { commands, Config } from '@circleci/circleci-config-sdk';
import { config } from '../../config';
import { AbstractTestJob } from './abstract-job-test';
import { UbuntuExecutor } from '../../executors';

export class TestRepositoryJob extends AbstractTestJob {
  public static create(dynamicConfig: Config) {
    return super.create(
      dynamicConfig,
      'job-test-repository',
      new commands.Run({
        name: `Run repository tests`,
        // Need to use `verify` phase to get repo-test's jar build and shared to mongodb and jdbc repos
        // and then collect and merge all coverage reports
        command: `mvn --fail-fast -s ${config.maven.settingsFile} verify --no-transfer-progress -Drepository-modules -Dskip.validation=true -T 2C`,
      }),
      UbuntuExecutor.create('large'),
      ['gravitee-apim-repository/gravitee-apim-repository-coverage/target/site/jacoco-aggregate/'],
    );
  }
}
