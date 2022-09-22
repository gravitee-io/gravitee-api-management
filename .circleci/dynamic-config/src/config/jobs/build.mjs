import { cache, Checkout, Job, ReusedCommand, Run, workspace } from '../../sdk/index.mjs';

export function createBuildJob(executor) {
  return new Job('build', executor, [
    new Checkout(),
    new workspace.Attach({ at: '.' }),
    new ReusedCommand('restore-maven-job-cache', {
      jobName: 'build',
    }),
    new ReusedCommand('prepare-env-var'),
    new Run({
      name: 'Build project',
      command: [
        'mvn -s .gravitee.settings.xml clean install --no-transfer-progress --update-snapshots -DskipTests -Dskip.validation=true -T 2C -Ddistribution-dev',
        'mkdir -p ./rest-api-docker-context/distribution && cp -r ./gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target/distribution ./rest-api-docker-context/.',
        'mkdir -p ./gateway-docker-context/distribution && cp -r ./gravitee-apim-gateway/gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-distribution/target/distribution ./gateway-docker-context/.',
      ].join('\n'),
    }),
    new ReusedCommand('save-maven-job-cache', {
      jobName: 'build',
    }),
    new cache.Save({
      paths: ['~/.m2/repository/io/gravitee/apim'],
      key: 'gravitee-api-management-v8-build-apim-{{ .Environment.CIRCLE_WORKFLOW_WORKSPACE_ID }}',
      when: 'on_success',
    }),
    new workspace.Persist({
      root: './',
      paths: ['./gravitee-*/*/target/*', './rest-api-docker-context', './gateway-docker-context'],
    }),
  ]);
}
