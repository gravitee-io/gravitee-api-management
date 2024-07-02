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
        baseBranch: 'master',
        branch: 'master',
        sha1: '784ff35ca',
        changedFiles: [],
        buildNum: '1234',
        buildId: '1234',
        graviteeioVersion,
        isDryRun,
        apimVersionPath: '',
      });

      const expected = fs.readFileSync(`./src/pipelines/tests/resources/release-notes-apim/${expectedFileName}`, 'utf-8');
      expect(result.stringify()).toStrictEqual(expected);
    },
  );

  it('should throw exception if Gravitee version is not specified', function () {
    expect.assertions(1);

    try {
      generateReleaseNotesApimConfig({
        action: 'release_notes_apim',
        baseBranch: 'master',
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
