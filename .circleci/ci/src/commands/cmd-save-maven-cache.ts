import { commands, parameters, reusable } from '@circleci/circleci-config-sdk';
import { config } from '../config';

export class SaveMavenJobCacheCommand {
  public static commandName = 'cmd-save-maven-job-cache';

  public static get(): reusable.ReusableCommand {
    return new reusable.ReusableCommand(
      SaveMavenJobCacheCommand.commandName,
      [
        new commands.Run({
          name: 'Exclude all APIM artefacts from cache.',
          command: 'rm -rf ~/.m2/repository/io/gravitee/apim',
        }),
        new commands.cache.Save({
          key: `${config.cache.prefix}-<< parameters.jobName >>-{{ .Branch }}-{{ checksum "pom.xml" }}`,
          paths: ['~/.m2'],
          when: 'always',
        }),
      ],
      new parameters.CustomParametersList([new parameters.CustomParameter('jobName', 'string', '', 'The job name')]),
      'Save Maven cache for a dedicated job',
    );
  }
}
