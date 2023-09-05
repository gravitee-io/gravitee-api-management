import { commands, Config, parameters } from '@circleci/circleci-config-sdk';
import { config } from '../../config';
import { AbstractTestContainerJob } from './abstract-job-test-container';

export class MongoTestContainerJob extends AbstractTestContainerJob {
  public static create(dynamicConfig: Config) {
    return super.create(
      dynamicConfig,
      'job-mongo-test-container',
      new parameters.CustomParametersList([new parameters.CustomParameter('mongoVersion', 'string', '', 'Version of Mongo to test')]),
      new commands.Run({
        name: 'Run Management repository tests with MongoDB << parameters.mongoVersion >>',
        command: `cd gravitee-apim-repository
mvn -pl 'gravitee-apim-repository-mongodb' -am -s ../${config.maven.settingsFile} clean package --no-transfer-progress -Dskip.validation=true -DmongoVersion=<< parameters.mongoVersion>> -T 2C`,
      }),
    );
  }
}
