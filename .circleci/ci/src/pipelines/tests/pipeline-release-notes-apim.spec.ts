import fs from 'fs';
import { generateReleaseNotesApimConfig } from '../pipeline-release-notes-apim';

describe('Release notes apim workflow tests', () => {
  it.each`
    graviteeioVersion | isDryRun | expectedFileName
    ${'4.1.0'}        | ${true}  | ${'release-notes-apim-dry-run.yml'}
    ${'4.1.0'}        | ${false} | ${'release-notes-apim-no-dry-run.yml'}
  `(
    'should build release notes apim with $graviteeioVersion and dry run as $isDryRun',
    ({ graviteeioVersion, isDryRun, expectedFileName }) => {
      const result = generateReleaseNotesApimConfig({
        action: 'release_notes_apim',
        branch: 'master',
        sha1: '784ff35ca',
        changedFiles: [],
        buildNum: '1234',
        buildId: '1234',
        graviteeioVersion,
        isDryRun,
      });

      const expected = fs.readFileSync(`./src/pipelines/tests/resources/release-notes-apim/${expectedFileName}`, 'utf-8');
      expect(expected).toStrictEqual(result.stringify());
    },
  );

  it('should throw exception if Gravitee version is not specified', function () {
    expect.assertions(1);

    try {
      generateReleaseNotesApimConfig({
        action: 'release_notes_apim',
        branch: 'master',
        sha1: '784ff35ca',
        changedFiles: [],
        isDryRun: false,
        graviteeioVersion: '',
        buildNum: '1234',
        buildId: '1234',
      });
    } catch (e) {
      expect(e).toStrictEqual(new Error('Graviteeio version is not defined - Please export CI_GRAVITEEIO_VERSION environment variable'));
    }
  });
});
