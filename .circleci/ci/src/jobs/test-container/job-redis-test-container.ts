import { commands, Config, parameters } from '@circleci/circleci-config-sdk';
import { config } from '../../config';
import { AbstractTestContainerJob } from './abstract-job-test-container';

export class RedisTestContainerJob extends AbstractTestContainerJob {
  public static create(dynamicConfig: Config) {
    return super.create(
      dynamicConfig,
      'job-redis-test-container',
      new parameters.CustomParametersList([new parameters.CustomParameter('redisVersion', 'string', '', 'Version of Redis to test')]),
      new commands.Run({
        name: 'Run Rate-limit repository tests with Redis << parameters.redisVersion >>',
        command: `cd gravitee-apim-repository
mvn -pl 'gravitee-apim-repository-redis' -am -s ../${config.maven.settingsFile} clean package --no-transfer-progress -Dskip.validation=true -DredisVersion=<< parameters.redisVersion >>`,
      }),
    );
  }
}
