import { ReusableCommand, cache, CustomParametersList, CustomParameter, Run } from '../../sdk/index.mjs';

export function createSaveMavenCacheCommand() {
  return new ReusableCommand(
    `save-maven-job-cache`,
    [
      new Run({
        name: 'Exclude all APIM artifacts from cache.',
        command: 'rm -rf ~/.m2/repository/io/gravitee/apim',
      }),
      new cache.Save({
        paths: ['~/.m2'],
        key: 'gravitee-api-management-v8-<< parameters.jobName >>-{{ .Branch }}-{{ checksum "pom.xml" }}',
        when: 'always',
      }),
    ],
    new CustomParametersList([new CustomParameter('jobName', 'string', '', 'The job name')]),
    'Save Maven cache for a dedicated job',
  );
}
