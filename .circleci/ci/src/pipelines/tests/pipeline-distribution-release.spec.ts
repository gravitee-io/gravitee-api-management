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
import { generateDistributionReleaseConfig } from '../pipeline-distribution-release';

describe('Distribution release tests', () => {
  const guardOf = (version: string) =>
    generateDistributionReleaseConfig({
      action: 'distribution_release',
      baseBranch: 'master',
      branch: '',
      sha1: '784ff35ca',
      changedFiles: [],
      buildNum: '1234',
      buildId: '1234',
      graviteeioVersion: version,
      tag: version,
      isDryRun: false,
      apimVersionPath: './src/pipelines/tests/resources/common/pom.xml',
    }).stringify();

  it('should refuse a core pin that is a SNAPSHOT', () => {
    expect(guardOf('4.12.16')).toContain('A release cannot assemble a SNAPSHOT core');
  });

  it('should refuse a core pin from another version line', () => {
    // The pin may trail by patches, so the comparison is on the line rather than on the version.
    expect(guardOf('4.12.16')).toContain('"${PIN_BASE%.*}" != "4.12"');
  });

  it('should check the pin before anything is built', () => {
    // Behind the engine build it fired roughly half an hour into the release, and a mispinned
    // release is the expected first outcome after a code freeze.
    const generated = guardOf('4.12.16');

    expect(generated.indexOf('Refuse a core pin this release cannot assemble')).toBeLessThan(generated.indexOf('Maven build APIM engine'));
  });

  it('should take the line from a qualified version too', () => {
    expect(guardOf('4.13.0-alpha.1')).toContain('"${PIN_BASE%.*}" != "4.13"');
  });

  it.each`
    baseBranch | isDryRun | dockerTagAsLatest | graviteeioVersion   | apimVersionPath                                              | expectedResult
    ${'4.2.x'} | ${true}  | ${false}          | ${'4.2.0'}          | ${'./src/pipelines/tests/resources/common/pom-snapshot.xml'} | ${'release-4-2-0-dry-run.yml'}
    ${'4.2.x'} | ${false} | ${false}          | ${'4.2.0'}          | ${'./src/pipelines/tests/resources/common/pom-snapshot.xml'} | ${'release-4-2-0-no-dry-run.yml'}
    ${'4.2.x'} | ${false} | ${true}           | ${'4.2.0'}          | ${'./src/pipelines/tests/resources/common/pom-snapshot.xml'} | ${'release-4-2-0-latest.yml'}
    ${'4.2.x'} | ${false} | ${false}          | ${'4.2.0-alpha.1'}  | ${'./src/pipelines/tests/resources/common/pom-alpha.xml'}    | ${'release-4-2-0-alpha.yml'}
    ${'4.2.x'} | ${false} | ${false}          | ${'4.2.0-hotfix.1'} | ${'./src/pipelines/tests/resources/common/pom-hotfix.xml'}   | ${'release-4-2-0-hotfix.yml'}
  `(
    'should build the distribution release config with dry run $isDryRun, is latest $dockerTagAsLatest and version $graviteeioVersion',
    ({ baseBranch, isDryRun, dockerTagAsLatest, graviteeioVersion, apimVersionPath, expectedResult }) => {
      const result = generateDistributionReleaseConfig({
        action: 'distribution_release',
        sha1: '784ff35ca',
        changedFiles: [],
        buildNum: '1234',
        buildId: '1234',
        apimVersionPath,
        graviteeioVersion,
        tag: graviteeioVersion,
        baseBranch,
        branch: '',
        isDryRun,
        dockerTagAsLatest,
      });

      const expected = fs.readFileSync(`./src/pipelines/tests/resources/distribution-release/${expectedResult}`, 'utf-8');
      expect(result.stringify()).toStrictEqual(expected);
    },
  );

  it('should derive the support line, since a tag build carries no branch', () => {
    const result = generateDistributionReleaseConfig({
      action: 'distribution_release',
      sha1: '784ff35ca',
      changedFiles: [],
      buildNum: '1234',
      buildId: '1234',
      graviteeioVersion: '4.1.0',
      tag: '4.1.0',
      branch: '',
      baseBranch: 'master',
      isDryRun: false,
      apimVersionPath: './src/pipelines/tests/resources/common/pom-snapshot.xml',
    });

    const stringified = result.stringify();
    expect(stringified).toContain('distribution_release');
    // What the SaaS trigger sends onward as a directory name. An empty one lands in a folder with
    // no Dockerfile, and the prod image builds fail — taking the rest of the lane with them.
    expect(stringified).toContain('"branch-version":"4.1.x"');
  });
});
