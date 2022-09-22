import { cache, Checkout, Job, ReusedCommand, Run, workspace } from '../../sdk/index.mjs';

export function createTestJob(executor, jobName, modules) {
  return new Job(jobName, executor, [
    new Checkout(),
    new workspace.Attach({ at: '.' }),
    new ReusedCommand('restore-maven-job-cache', {
      jobName,
    }),
    new cache.Restore({
      keys: ['gravitee-api-management-v8-build-apim-{{ .Environment.CIRCLE_WORKFLOW_WORKSPACE_ID }}'],
    }),
    new Run({
      name: 'Run tests',
      command: `mvn -U --fail-fast -s .gravitee.settings.xml test --no-transfer-progress -D${modules} -Dskip.validation=true -T 2C`,
    }),
    new Run({
      name: 'Save test results',
      command: [
        'mkdir -p ~/test-results/junit/',
        'find . -type f -regex ".*/target/surefire-reports/.*xml" -exec cp {} ~/test-results/junit/ ;',
      ].join('\n'),
      when: 'always',
    }),
    new ReusedCommand('notify-on-failure'),
    new ReusedCommand('save-maven-job-cache', {
      jobName,
    }),
    new ReusedCommand('store_test_results', {
      path: '~/test-results',
    }),
    new workspace.Persist({
      root: '.',
      paths: [
        'gravitee-apim-gateway/gravitee-apim-gateway-coverage/target/site/jacoco-aggregate/',
        'gravitee-apim-rest-api/gravitee-apim-rest-api-coverage/target/site/jacoco-aggregate/',
        'gravitee-apim-definition/gravitee-apim-definition-coverage/target/site/jacoco-aggregate/',
      ],
    }),
  ]);
}
