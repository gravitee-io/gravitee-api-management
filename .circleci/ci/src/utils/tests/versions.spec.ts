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
import { computeApimVersion, parse, validateGraviteeioVersion } from '../versions';

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
      } catch (e) {
        fail('Should not throw exception');
      }
    });
  });
});
