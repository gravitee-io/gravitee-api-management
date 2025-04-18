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
import { generateBuildDockerImagesConfig } from '../pipeline-build-docker-images';

describe('Build Docker images workflow tests', () => {
  it.each`
    graviteeioVersion  | isDryRun | dockerTagAsLatest | apimVersionPath                                           | expectedFileName
    ${'4.2.0-alpha.1'} | ${true}  | ${false}          | ${'./src/pipelines/tests/resources/common/pom-alpha.xml'} | ${'build-docker-images-prerelease-dry-run.yml'}
    ${'4.2.0-alpha.1'} | ${false} | ${false}          | ${'./src/pipelines/tests/resources/common/pom-alpha.xml'} | ${'build-docker-images-prerelease-no-dry-run.yml'}
    ${'4.2.0'}         | ${true}  | ${false}          | ${'./src/pipelines/tests/resources/common/pom.xml'}       | ${'build-docker-images-release-dry-run.yml'}
    ${'4.2.0'}         | ${false} | ${false}          | ${'./src/pipelines/tests/resources/common/pom.xml'}       | ${'build-docker-images-release-no-dry-run.yml'}
    ${'4.2.0'}         | ${false} | ${true}           | ${'./src/pipelines/tests/resources/common/pom.xml'}       | ${'build-docker-images-release-no-dry-run-as-latest.yml'}
  `(
    'should build RPM & Docker images with $graviteeioVersion and dry run as $isDryRun',
    ({ graviteeioVersion, isDryRun, dockerTagAsLatest, apimVersionPath, expectedFileName }) => {
      const result = generateBuildDockerImagesConfig({
        action: 'build_docker_images',
        baseBranch: 'master',
        branch: 'master',
        sha1: '784ff35ca',
        changedFiles: [],
        buildNum: '1234',
        buildId: '1234',
        graviteeioVersion,
        isDryRun,
        dockerTagAsLatest,
        apimVersionPath,
      });

      const expected = fs.readFileSync(`./src/pipelines/tests/resources/build-docker-images/${expectedFileName}`, 'utf-8');
      expect(result.stringify()).toStrictEqual(expected);
    },
  );

  it('should throw an error when trying to generate build Docker images config without graviteeio version', () => {
    expect.assertions(1);

    try {
      generateBuildDockerImagesConfig({
        action: 'build_docker_images',
        baseBranch: 'master',
        branch: 'master',
        sha1: '784ff35ca',
        changedFiles: [],
        buildNum: '1234',
        buildId: '1234',
        isDryRun: false,
        graviteeioVersion: '',
        dockerTagAsLatest: false,
        apimVersionPath: '',
      });
    } catch (e) {
      expect(e).toStrictEqual(new Error('Graviteeio version is not defined - Please export CI_GRAVITEEIO_VERSION environment variable'));
    }
  });

  it('should throw an error when trying to generate build Docker images config without branch', () => {
    expect.assertions(1);

    try {
      generateBuildDockerImagesConfig({
        action: 'build_docker_images',
        baseBranch: '',
        branch: '',
        sha1: '784ff35ca',
        changedFiles: [],
        buildNum: '1234',
        buildId: '1234',
        isDryRun: false,
        graviteeioVersion: '1.2.3',
        dockerTagAsLatest: false,
        apimVersionPath: '',
      });
    } catch (e) {
      expect(e).toStrictEqual(new Error('A branch (CIRCLE_BRANCH) must be specified'));
    }
  });
});
