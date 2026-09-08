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
import { generatePrepareCoreReleaseConfig } from '../pipeline-prepare-core-release';
import { CircleCIEnvironment } from '../circleci-environment';

const environment: CircleCIEnvironment = {
  action: 'prepare_core_release',
  baseBranch: 'master',
  branch: '4.13.x',
  sha1: '784ff35ca',
  changedFiles: [],
  buildNum: '1234',
  buildId: '1234',
  graviteeioVersion: '4.13.0',
  isDryRun: false,
  apimVersionPath: '',
};

const tagStep = (config: { stringify: () => string }) => config.stringify();

describe('Prepare core release workflow tests', () => {
  it('should commit, tag and reopen the branch', function () {
    const result = generatePrepareCoreReleaseConfig(environment);

    const expected = fs.readFileSync('./src/pipelines/tests/resources/prepare-core-release/prepare-core-release.yml', 'utf-8');
    expect(result.stringify()).toStrictEqual(expected);
  });

  it('should tag with the core_ prefix, leaving the bare version to the distribution', function () {
    expect(tagStep(generatePrepareCoreReleaseConfig(environment))).toContain('git tag core_4.13.0');
  });

  it('should push the branch before the tag', function () {
    const generated = tagStep(generatePrepareCoreReleaseConfig(environment));

    expect(generated.indexOf('git push  origin 4.13.x')).toBeLessThan(generated.indexOf('git push  origin core_4.13.0'));
  });

  it('should reopen a final release on the next patch', function () {
    expect(tagStep(generatePrepareCoreReleaseConfig(environment))).toContain('<revision>4.13.1</revision>');
  });

  it('should reopen a qualified release on the next qualifier, keeping the number', function () {
    const generated = tagStep(generatePrepareCoreReleaseConfig({ ...environment, graviteeioVersion: '4.13.0-alpha.1' }));

    expect(generated).toContain('<revision>4.13.0</revision>');
    expect(generated).toContain('<sha1>-alpha.2</sha1>');
  });

  it('should leave the distribution pom alone', function () {
    expect(tagStep(generatePrepareCoreReleaseConfig(environment))).not.toContain('gravitee-apim-distribution/pom.xml');
  });

  it('should push nothing on a dry run', function () {
    expect(tagStep(generatePrepareCoreReleaseConfig({ ...environment, isDryRun: true }))).toContain('git push --dry-run origin 4.13.x');
  });

  it('should throw when the version is missing', function () {
    expect(() => generatePrepareCoreReleaseConfig({ ...environment, graviteeioVersion: '' })).toThrow(
      new Error('Graviteeio version is not defined - Please export CI_GRAVITEEIO_VERSION environment variable'),
    );
  });
});
