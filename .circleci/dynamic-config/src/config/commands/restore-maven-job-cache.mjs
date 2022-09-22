import { ReusableCommand, cache, CustomParametersList, CustomParameter } from '../../sdk/index.mjs';

export function createRestoreMavenCacheCommand() {
  return new ReusableCommand(
    `restore-maven-job-cache`,
    [
      new cache.Restore({
        keys: [
          'gravitee-api-management-v8-<< parameters.jobName >>-{{ .Branch }}-{{ checksum "pom.xml" }}',
          'gravitee-api-management-v8-<< parameters.jobName >>-{{ .Branch }}-',
          'gravitee-api-management-v8-<< parameters.jobName >>-',
        ],
      }),
    ],
    new CustomParametersList([new CustomParameter('jobName', 'string', '', 'The job name')]),
    'Restore Maven cache for a dedicated job',
  );
}
