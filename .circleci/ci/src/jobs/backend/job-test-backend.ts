import { commands, Config } from '@circleci/circleci-config-sdk';
import { config } from '../../config';
import { AbstractTestJob } from './abstract-job-test';
import { OpenJdkExecutor } from '../../executors';

export class TestBackendJob extends AbstractTestJob {
  public static create(dynamicConfig: Config) {
    return super.create(
      dynamicConfig,
      'job-test-backend',
      new commands.Run({
        name: `Run backend tests`,
        command: `mvn --fail-fast -s ${config.maven.settingsFile} test --no-transfer-progress -Dmain-modules -Dskip.validation=true -T 2C`,
      }),
      OpenJdkExecutor.create('medium+'),
      [
        'gravitee-apim-gateway/gravitee-apim-gateway-coverage/target/site/jacoco-aggregate/',
        'gravitee-apim-rest-api/gravitee-apim-rest-api-coverage/target/site/jacoco-aggregate/',
        'gravitee-apim-definition/gravitee-apim-definition-coverage/target/site/jacoco-aggregate/',
      ],
    );
  }
}
