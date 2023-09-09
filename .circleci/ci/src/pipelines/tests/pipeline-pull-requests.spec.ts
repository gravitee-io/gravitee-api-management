import * as fs from 'fs';
import { generatePullRequestsConfig } from '../pipeline-pull-requests';

describe('Pull requests workflow tests', () => {
  it.each`
    branchName                      | expectedFileName
    ${'master'}                     | ${'pull-requests-master.yml'}
    ${'4.1.x'}                      | ${'pull-requests-4-1-x.yml'}
    ${'APIM-1234-my-custom-branch'} | ${'pull-requests-custom-branch.yml'}
    ${'APIM-1234-run-e2e'}          | ${'pull-requests-run-e2e.yml'}
    ${'mergify/bp/4.0.x/pr-1234'}   | ${'pull-requests-mergify.yml'}
  `('should generate pull-requests config fro branch $branchName', ({ branchName, expectedFileName }) => {
    const result = generatePullRequestsConfig({
      action: 'pull_requests',
      apimVersionPath: './src/pipelines/tests/resources/common/pom.xml',
      branch: branchName,
      sha1: '784ff35ca',
      changedFiles: [],
      buildNum: '1234',
      buildId: '1234',
      graviteeioVersion: '',
      isDryRun: false,
    });

    const expected = fs.readFileSync(`./src/pipelines/tests/resources/pull-requests/${expectedFileName}`, 'utf-8');
    expect(expected).toStrictEqual(result.stringify());
  });
});
