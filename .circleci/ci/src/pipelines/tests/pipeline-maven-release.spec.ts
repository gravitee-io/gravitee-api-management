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
import * as fs from 'fs';
import { generateMavenReleaseConfig } from '../pipeline-maven-release';

describe('Maven release workflow tests', () => {
  it('should build maven release with no dry run', function () {
    const result = generateMavenReleaseConfig({
      action: 'maven_release',
      baseBranch: 'master',
      branch: 'master',
      sha1: '784ff35ca',
      changedFiles: [],
      buildNum: '1234',
      buildId: '1234',
      graviteeioVersion: '1.2.3',
      isDryRun: false,
      apimVersionPath: '',
    });

    const expected = fs.readFileSync('./src/pipelines/tests/resources/maven-release/maven-release.yml', 'utf-8');
    expect(result.stringify()).toStrictEqual(expected);
  });

  it('should throw exception if dry run is true', function () {
    expect.assertions(1);

    try {
      generateMavenReleaseConfig({
        action: 'maven_release',
        baseBranch: 'master',
        branch: 'master',
        sha1: '784ff35ca',
        changedFiles: [],
        isDryRun: true,
        graviteeioVersion: '1.2.3',
        buildNum: '1234',
        buildId: '1234',
        apimVersionPath: '',
      });
    } catch (e) {
      expect(e).toStrictEqual(new Error('Dry Run - Maven release is deactivated if dry run is true'));
    }
  });

  it('should throw exception if Gravitee version is not specified', function () {
    expect.assertions(1);

    try {
      generateMavenReleaseConfig({
        action: 'maven_release',
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
