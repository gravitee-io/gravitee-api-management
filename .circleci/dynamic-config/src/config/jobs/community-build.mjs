import {
  cache,
  Checkout,
  CustomParameter,
  CustomParametersList,
  Job,
  OrbRef,
  ReusedCommand,
  Run,
  workspace,
} from '../../sdk/index.mjs';

export function createCommunityBuildJob(executor) {
  return new Job('community-build', executor, [
    new Checkout(),
    new ReusedCommand('restore-maven-job-cache', {
      jobName: 'community-build',
    }),
    new Run({
      name: 'Build project',
      command: 'mvn clean install --no-transfer-progress --update-snapshots -DskipTests -Dskip.validation=true -T 2C',
    }),
    new ReusedCommand('notify-on-failure'),
    new Run({
      name: 'Exclude APIM artifacts from build cache',
      command: 'rm -rf ~/.m2/repository/io/gravitee/apim',
    }),
    new ReusedCommand('save-maven-job-cache', {
      jobName: 'community-build',
    }),
  ]);
}
