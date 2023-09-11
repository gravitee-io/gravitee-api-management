import * as fs from 'fs';
import { generateReleaseConfig } from '../pipeline-release';

describe('Release tests', () => {
  it.each`
    branch     | isDryRun | apimVersionPath                                              | graviteeioVersion  | expectedResult
    ${'4.2.x'} | ${true}  | ${'./src/pipelines/tests/resources/common/pom.xml'}          | ${'4.2.0'}         | ${'release-4-2-0-dry-run.yml'}
    ${'4.2.x'} | ${false} | ${'./src/pipelines/tests/resources/common/pom-snapshot.xml'} | ${'4.2.0'}         | ${'release-4-2-0-snapshot.yml'}
    ${'4.2.x'} | ${false} | ${'./src/pipelines/tests/resources/common/pom-alpha.xml'}    | ${'4.2.0-alpha.1'} | ${'release-4-2-0-alpha.yml'}
  `(
    'should build release config on $branch with dry run $isDryRun and graviteeio version $graviteeioVersion',
    ({ branch, isDryRun, apimVersionPath, graviteeioVersion, expectedResult }) => {
      const result = generateReleaseConfig({
        action: 'release',
        sha1: '784ff35ca',
        changedFiles: [],
        buildNum: '1234',
        buildId: '1234',
        graviteeioVersion,
        branch,
        apimVersionPath,
        isDryRun,
      });

      const expected = fs.readFileSync(`./src/pipelines/tests/resources/release/${expectedResult}`, 'utf-8');
      expect(expected).toStrictEqual(result.stringify());
    },
  );

  it('should throw error when branch is not a support branch', () => {
    expect.assertions(1);

    try {
      generateReleaseConfig({
        action: 'release',
        sha1: '784ff35ca',
        changedFiles: [],
        buildNum: '1234',
        buildId: '1234',
        graviteeioVersion: '4.1.0',
        branch: 'apim-1234-dev',
        apimVersionPath: '/some/path',
        isDryRun: false,
      });
    } catch (e) {
      expect(e).toStrictEqual(new Error('Release is only supported on support branches'));
    }
  });
});
