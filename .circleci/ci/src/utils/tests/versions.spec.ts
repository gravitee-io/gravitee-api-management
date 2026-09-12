/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { computeApimVersion, isLatestRelease, nextDevelopmentVersion, parse, validateGraviteeioVersion } from '../versions';

describe('version', function () {
  describe('parse', function () {
    it.each`
      versionToParse     | versionFull | major  | minor  | patch  | qualifierFull | qualifierName | qualifierVersion
      ${'1.2.3-alpha.4'} | ${'1.2.3'}  | ${'1'} | ${'2'} | ${'3'} | ${'alpha.4'}  | ${'alpha'}    | ${'4'}
      ${'1.2.3'}         | ${'1.2.3'}  | ${'1'} | ${'2'} | ${'3'} | ${''}         | ${''}         | ${''}
    `(
      'returns parsed version $versionToParse',
      ({ versionToParse, versionFull, major, minor, patch, qualifierFull, qualifierName, qualifierVersion }) => {
        expect(parse(versionToParse)).toEqual({
          full: versionToParse,
          version: {
            full: versionFull,
            major,
            minor,
            patch,
          },
          qualifier: {
            full: qualifierFull,
            name: qualifierName,
            version: qualifierVersion,
          },
        });
      },
    );
  });

  describe('computeApimVersion', () => {
    it.each`
      apimVersionPath                                        | expected
      ${'./src/utils/tests/resources/pom.xml'}               | ${'4.2.0'}
      ${'./src/utils/tests/resources/pom-sha1-snapshot.xml'} | ${'4.2.0-alpha.1-SNAPSHOT'}
      ${'./src/utils/tests/resources/pom-snapshot.xml'}      | ${'4.2.0-SNAPSHOT'}
    `('should parse apim version', ({ apimVersionPath, expected }) => {
      expect(
        computeApimVersion({
          action: 'package_bundle',
          baseBranch: 'master',
          branch: 'master',
          sha1: '784ff35ca',
          changedFiles: [],
          buildNum: '1234',
          buildId: '1234',
          graviteeioVersion: '4.2.0',
          isDryRun: false,
          apimVersionPath,
        }),
      ).toStrictEqual(expected);
    });

    it('should throw exception with non-existing file', () => {
      expect.assertions(1);

      try {
        computeApimVersion({
          action: 'package_bundle',
          baseBranch: 'master',
          branch: 'master',
          sha1: '784ff35ca',
          changedFiles: [],
          buildNum: '1234',
          buildId: '1234',
          graviteeioVersion: '4.2.0',
          isDryRun: false,
          apimVersionPath: './path/to/non-existing/file',
        });
      } catch (e) {
        expect(e).toStrictEqual(new Error('computeApiVersion - No file at specified path: ./path/to/non-existing/file'));
      }
    });
  });

  describe('validateGraviteeioVersion', () => {
    it('should throw exception with blank graviteeio version', () => {
      expect.assertions(1);

      try {
        validateGraviteeioVersion('');
      } catch (e) {
        expect(e).toStrictEqual(new Error('Graviteeio version is not defined - Please export CI_GRAVITEEIO_VERSION environment variable'));
      }
    });

    it('should not throw exception with valid graviteeio version', () => {
      try {
        validateGraviteeioVersion('1.2.3-alpha.4');
      } catch {
        fail('Should not throw exception');
      }
    });
  });
});

describe('nextDevelopmentVersion', () => {
  it.each([
    ['4.13.0', '4.13.1', ''],
    ['4.12.17', '4.12.18', ''],
    ['4.12.9', '4.12.10', ''],
  ])('opens the next patch after the final release %s', (released, version, qualifier) => {
    expect(nextDevelopmentVersion(released)).toEqual({ version, qualifier });
  });

  it.each([
    ['4.13.0-alpha.1', '4.13.0', '-alpha.2'],
    ['4.13.0-rc.9', '4.13.0', '-rc.10'],
    ['4.12.17-hotfix.1', '4.12.17', '-hotfix.2'],
  ])('increments the qualifier after %s, keeping the number', (released, version, qualifier) => {
    expect(nextDevelopmentVersion(released)).toEqual({ version, qualifier });
  });
});

describe('isLatestRelease', function () {
  // The tag that triggers a distribution release carries no pipeline parameter, so which images
  // take `latest` is read from what has already been released rather than typed at a prompt.
  const released = ['4.11.28', '4.12.19', '4.12.20', '4.13.0-alpha.1', 'core_4.13.0'];

  it('is true for the highest final release', function () {
    expect(isLatestRelease('4.13.0', [...released, '4.13.0'])).toBe(true);
  });

  it('is false for a patch on a line an older minor has overtaken', function () {
    expect(isLatestRelease('4.11.29', [...released, '4.11.29'])).toBe(false);
  });

  // An alpha must never become what `docker pull` hands to someone who names no tag.
  it('is false for a qualified version, however high', function () {
    expect(isLatestRelease('5.0.0-alpha.1', released)).toBe(false);
  });

  it('ignores qualified and prefixed tags when looking for something higher', function () {
    expect(isLatestRelease('4.12.20', ['4.12.20', '4.13.0-alpha.9', 'core_5.0.0'])).toBe(true);
  });

  it('compares numerically, not as strings', function () {
    expect(isLatestRelease('4.12.9', ['4.12.9', '4.12.10'])).toBe(false);
    expect(isLatestRelease('4.9.0', ['4.9.0', '4.10.0'])).toBe(false);
  });

  it('is true for the very first release, with nothing to compare against', function () {
    expect(isLatestRelease('1.0.0', [])).toBe(true);
  });
});
