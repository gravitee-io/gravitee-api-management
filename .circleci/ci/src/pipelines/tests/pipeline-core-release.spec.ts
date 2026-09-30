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
import { generateCoreReleaseConfig } from '../pipeline-core-release';
import { CircleCIEnvironment } from '../circleci-environment';

const environment: CircleCIEnvironment = {
  action: 'core_release',
  baseBranch: 'master',
  branch: '',
  sha1: '784ff35ca',
  changedFiles: [],
  buildNum: '1234',
  buildId: '1234',
  graviteeioVersion: '4.13.0',
  tag: 'core_4.13.0',
  isDryRun: false,
  apimVersionPath: './src/pipelines/tests/resources/common/pom.xml',
};

describe('Core release workflow tests', () => {
  it('should publish the tag it was started by', function () {
    const result = generateCoreReleaseConfig(environment);

    const expected = fs.readFileSync('./src/pipelines/tests/resources/core-release/core-release.yml', 'utf-8');
    expect(result.stringify()).toStrictEqual(expected);
  });

  it('should commit the pin on the branch the version comes from', function () {
    const generated = generateCoreReleaseConfig(environment).stringify();

    expect(generated).toContain('git checkout -B 4.13.x --no-track origin/4.13.x');
    expect(generated).toContain('git push origin 4.13.x');
  });

  it('should keep the pin commit out of CI', function () {
    const generated = generateCoreReleaseConfig(environment).stringify();

    expect(generated).toContain("git commit -m 'chore(distribution): pin core 4.13.0 [skip ci]'");
  });

  it('should leave nothing to review', function () {
    const generated = generateCoreReleaseConfig(environment).stringify();

    expect(generated).not.toContain('gh pr');
    expect(generated).not.toContain('chore/pin-core');
  });

  it('should advance the pin onto the version just published', function () {
    expect(generateCoreReleaseConfig(environment).stringify()).toContain(
      '<apim.core.version>4.13.0</apim.core.version>#" gravitee-apim-distribution/pom.xml',
    );
  });

  it('should pin only after the core has been published', function () {
    const generated = generateCoreReleaseConfig(environment).stringify();

    expect(generated.indexOf('Pin the core')).toBeGreaterThan(generated.indexOf('Publish release'));
  });

  it('should refuse to believe a pin edit that did not take', function () {
    const generated = generateCoreReleaseConfig(environment).stringify();

    expect(generated.indexOf('grep -q "<apim.core.version>4.13.0</apim.core.version>"')).toBeLessThan(
      generated.indexOf('git diff --quiet'),
    );
  });

  it('should throw when no tag started the pipeline', function () {
    expect(() => generateCoreReleaseConfig({ ...environment, tag: undefined })).toThrow(
      new Error('Core release is triggered by a tag, and no tag started this pipeline'),
    );
  });

  it('should throw when the version is missing', function () {
    expect(() => generateCoreReleaseConfig({ ...environment, graviteeioVersion: '' })).toThrow(
      new Error('Graviteeio version is not defined - Please export CI_GRAVITEEIO_VERSION environment variable'),
    );
  });
});
