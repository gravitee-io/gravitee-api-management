import * as fs from 'fs';
import { generateReleaseHelmConfig } from '../pipeline-release-helm';

describe('Release helm charts tests', () => {
  it.each`
    isDryRun | expectedResult
    ${false} | ${'release-helm.yml'}
    ${true}  | ${'release-helm-dry-run.yml'}
  `('should build release helm chart with dry run $isDryRun', ({ isDryRun, expectedResult }) => {
    const result = generateReleaseHelmConfig({
      action: 'release_helm',
      sha1: '784ff35ca',
      changedFiles: [],
      buildNum: '1234',
      buildId: '1234',
      graviteeioVersion: '4.2.0',
      apimVersionPath: './src/pipelines/tests/resources/common/pom.xml',
      branch: 'master',
      isDryRun,
    });

    const expected = fs.readFileSync(`./src/pipelines/tests/resources/release-helm/${expectedResult}`, 'utf-8');
    expect(expected).toStrictEqual(result.stringify());
  });
});
