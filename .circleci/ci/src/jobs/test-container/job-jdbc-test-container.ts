import { commands, Config, parameters } from '@circleci/circleci-config-sdk';
import { config } from '../../config';
import { UbuntuExecutor } from '../../executors';
import { AbstractTestContainerJob } from './abstract-job-test-container';

export class JdbcTestContainerJob extends AbstractTestContainerJob {
  public static create(dynamicConfig: Config) {
    return super.create(
      dynamicConfig,
      'job-jdbc-test-container',
      new parameters.CustomParametersList([
        new parameters.CustomParameter('jdbcType', 'string', '', 'Type and version of the database to test. Example: mariadb:10.5'),
      ]),
      new commands.Run({
        name: 'Run Management repository tests with database << parameters.jdbcType >>',
        command: `cd gravitee-apim-repository
mvn -pl 'gravitee-apim-repository-jdbc' -am -s ../${config.maven.settingsFile} clean package --no-transfer-progress -Dskip.validation=true -DjdbcType=<< parameters.jdbcType>> -T 2C`,
      }),
      UbuntuExecutor.create('medium', true),
    );
  }
}
