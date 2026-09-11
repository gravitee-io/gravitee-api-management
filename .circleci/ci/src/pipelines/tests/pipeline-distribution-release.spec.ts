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
      branch: 'master',
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

  describe('APIM API docs ingestion', () => {
    const generated = () =>
      generateDistributionReleaseConfig({
        action: 'distribution_release',
        baseBranch: '4.2.x',
        branch: '4.2.x',
        sha1: '784ff35ca',
        changedFiles: [],
        buildNum: '1234',
        buildId: '1234',
        graviteeioVersion: '4.2.0',
        tag: '4.2.0',
        isDryRun: false,
        apimVersionPath: './src/pipelines/tests/resources/common/pom-snapshot.xml',
      }).stringify();

    // The site displays the product version; it fetches the jar under the core's. The two stop
    // being the same number the moment the reactors release apart.
    it('sends the version the docs site displays', () => {
      expect(generated()).toContain(`"version":"4.2.0"`);
    });

    it('sends the core version the docs site has to fetch', () => {
      expect(generated()).toContain(`"core_version":"'"\${CORE_VERSION}"'"`);
    });

    it('reads the pin from the distribution pom rather than guessing it', () => {
      expect(generated()).toContain('<apim.core.version>');
    });
  });

  it.each`
    baseBranch | branch            | isDryRun | dockerTagAsLatest | graviteeioVersion   | apimVersionPath                                              | expectedResult
    ${'4.2.x'} | ${'4.2.x'}        | ${true}  | ${false}          | ${'4.2.0'}          | ${'./src/pipelines/tests/resources/common/pom-snapshot.xml'} | ${'release-4-2-0-dry-run.yml'}
    ${'4.2.x'} | ${'4.2.x'}        | ${false} | ${false}          | ${'4.2.0'}          | ${'./src/pipelines/tests/resources/common/pom-snapshot.xml'} | ${'release-4-2-0-no-dry-run.yml'}
    ${'4.2.x'} | ${'4.2.x'}        | ${false} | ${true}           | ${'4.2.0'}          | ${'./src/pipelines/tests/resources/common/pom-snapshot.xml'} | ${'release-4-2-0-latest.yml'}
    ${'4.2.x'} | ${'4.2.x'}        | ${false} | ${false}          | ${'4.2.0-alpha.1'}  | ${'./src/pipelines/tests/resources/common/pom-alpha.xml'}    | ${'release-4-2-0-alpha.yml'}
    ${'4.2.x'} | ${'hotfix/4.2.0'} | ${false} | ${false}          | ${'4.2.0-hotfix.1'} | ${'./src/pipelines/tests/resources/common/pom-hotfix.xml'}   | ${'release-4-2-0-hotfix.yml'}
  `(
    'should build the distribution release config on $branch with dry run $isDryRun, is latest $dockerTagAsLatest and version $graviteeioVersion',
    ({ baseBranch, branch, isDryRun, dockerTagAsLatest, graviteeioVersion, apimVersionPath, expectedResult }) => {
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
        branch,
        isDryRun,
        dockerTagAsLatest,
      });

      const expected = fs.readFileSync(`./src/pipelines/tests/resources/distribution-release/${expectedResult}`, 'utf-8');
      expect(result.stringify()).toStrictEqual(expected);
    },
  );

  it('should build the distribution release config from any branch (not only support branches)', () => {
    const result = generateDistributionReleaseConfig({
      action: 'distribution_release',
      sha1: '784ff35ca',
      changedFiles: [],
      buildNum: '1234',
      buildId: '1234',
      graviteeioVersion: '4.1.0',
      tag: '4.1.0',
      branch: 'apim-1234-dev',
      baseBranch: 'master',
      isDryRun: false,
      apimVersionPath: './src/pipelines/tests/resources/common/pom-snapshot.xml',
    });

    const stringified = result.stringify();
    expect(stringified).toContain('distribution_release');
    // Nothing restricts which branch a tag may be released from, and the jobs that read the branch
    // still read the one the tag sits on.
    expect(stringified).toContain('apim-1234-dev');
  });
});
