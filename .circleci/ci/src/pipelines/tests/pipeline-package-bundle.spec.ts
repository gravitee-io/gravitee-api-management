import * as fs from 'fs';
import { generatePackageBundleConfig } from '../pipeline-package-bundle';

describe('Package bundle workflow tests', () => {
  it.each`
    graviteeioVersion  | isDryRun | expectedFileName
    ${'4.1.0-alpha.1'} | ${true}  | ${'package-bundle-prerelease-dry-run.yml'}
    ${'4.1.0-alpha.1'} | ${false} | ${'package-bundle-prerelease-no-dry-run.yml'}
    ${'4.1.0'}         | ${true}  | ${'package-bundle-release-dry-run.yml'}
    ${'4.1.0'}         | ${false} | ${'package-bundle-release-no-dry-run.yml'}
  `('should build package bundle with $graviteeioVersion', ({ graviteeioVersion, isDryRun, expectedFileName }) => {
    const result = generatePackageBundleConfig({
      action: 'package_bundle',
      branch: 'master',
      sha1: '784ff35ca',
      changedFiles: [],
      buildNum: '1234',
      buildId: '1234',
      graviteeioVersion,
      isDryRun,
      apimVersionPath: '',
    });

    const expected = fs.readFileSync(`./src/pipelines/tests/resources/package-bundle/${expectedFileName}`, 'utf-8');
    expect(expected).toStrictEqual(result.stringify());
  });

  it('should throw an error when trying to generate package bundle config without graviteeio version', () => {
    expect.assertions(1);

    try {
      generatePackageBundleConfig({
        action: 'package_bundle',
        branch: 'master',
        sha1: '784ff35ca',
        changedFiles: [],
        isDryRun: false,
        graviteeioVersion: '',
        buildNum: '1234',
        buildId: '1234',
        apimVersionPath: '',
      });
    } catch (e) {
      expect(e).toStrictEqual(new Error('Graviteeio version is not defined - Please export CI_GRAVITEEIO_VERSION environment variable'));
    }
  });
});
