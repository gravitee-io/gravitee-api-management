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
import { generateFullReleaseConfig } from '../pipeline-full-release';

describe('Full release tests', () => {
  const guardOf = (version: string) =>
    generateFullReleaseConfig({
      action: 'full_release',
      baseBranch: 'master',
      branch: 'master',
      sha1: '784ff35ca',
      changedFiles: [],
      buildNum: '1234',
      buildId: '1234',
      graviteeioVersion: version,
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

  describe('Nexus staging', () => {
    const configFor = (isDryRun: boolean) =>
      generateFullReleaseConfig({
        action: 'full_release',
        baseBranch: '4.2.x',
        branch: '4.2.x',
        sha1: '784ff35ca',
        changedFiles: [],
        buildNum: '1234',
        buildId: '1234',
        graviteeioVersion: '4.2.0',
        isDryRun,
        apimVersionPath: './src/pipelines/tests/resources/common/pom-snapshot.xml',
      }).stringify();

    const DEPLOY = 'mvn clean deploy --activate-profiles gravitee-release';

    it('publishes on a real release', () => {
      expect(configFor(false)).toContain(DEPLOY);
    });

    // The rehearsal has nothing to publish: the tag was pushed with --dry-run, so it does not exist.
    // The job used to run this deploy anyway and only failed on the missing tag — which would not
    // have saved a rehearsal of a version whose tag was already there.
    it('publishes nothing on a rehearsal', () => {
      expect(configFor(true)).not.toContain(DEPLOY);
    });

    it('does not check out a tag that was never pushed', () => {
      expect(configFor(true)).not.toContain('git checkout 4.2.0');
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
    'should build full release config on $branch with dry run $isDryRun, is latest $dockerTagAsLatest and version $graviteeioVersion',
    ({ baseBranch, branch, isDryRun, dockerTagAsLatest, graviteeioVersion, apimVersionPath, expectedResult }) => {
      const result = generateFullReleaseConfig({
        action: 'full_release',
        sha1: '784ff35ca',
        changedFiles: [],
        buildNum: '1234',
        buildId: '1234',
        apimVersionPath,
        graviteeioVersion,
        baseBranch,
        branch,
        isDryRun,
        dockerTagAsLatest,
      });

      const expected = fs.readFileSync(`./src/pipelines/tests/resources/full-release/${expectedResult}`, 'utf-8');
      expect(result.stringify()).toStrictEqual(expected);
    },
  );

  it('should build full release config from any branch (not only support branches)', () => {
    const result = generateFullReleaseConfig({
      action: 'full_release',
      sha1: '784ff35ca',
      changedFiles: [],
      buildNum: '1234',
      buildId: '1234',
      graviteeioVersion: '4.1.0',
      branch: 'apim-1234-dev',
      baseBranch: 'master',
      isDryRun: false,
      apimVersionPath: './src/pipelines/tests/resources/common/pom-snapshot.xml',
    });

    const stringified = result.stringify();
    expect(stringified).toContain('full_release');
    // The version bump commit is pushed to the very branch that triggered the release.
    expect(stringified).toContain('apim-1234-dev');
  });
});
