import { commands, Config, parameters } from '@circleci/circleci-config-sdk';
import { config } from '../../config';
import { AbstractTestContainerJob } from './abstract-job-test-container';

export class ElasticTestContainerJob extends AbstractTestContainerJob {
  public static create(dynamicConfig: Config) {
    return super.create(
      dynamicConfig,
      'job-elastic-test-container',
      new parameters.CustomParametersList([
        new parameters.CustomEnumParameter('engineType', ['elasticsearch', 'opensearch'], 'elasticsearch', 'Type of the search engine'),
        new parameters.CustomParameter('engineVersion', 'string', '', 'Version of engine to test'),
      ]),
      new commands.Run({
        name: `Run Analytics repository tests with engine << parameters.engineType >> and version << parameters.engineVersion >>`,
        command: `cd gravitee-apim-repository
mvn -pl 'gravitee-apim-repository-elasticsearch' -am -s ../${config.maven.settingsFile} clean package --no-transfer-progress -Dskip.validation=true -D<< parameters.engineType >>.version=<< parameters.engineVersion >> -Dsearch.type=<< parameters.engineType >> -T 2C`,
      }),
    );
  }
}
