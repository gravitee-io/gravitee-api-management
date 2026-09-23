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
import {
  bridgeClientTags,
  computeApimVersion,
  corePin,
  isLatestRelease,
  nextDevelopmentVersion,
  parse,
  supportLineOf,
  validateGraviteeioVersion,
} from '../versions';
import { CircleCIEnvironment } from '../../pipelines';

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

  describe('corePin', () => {
    const releaseOf = (apimVersionPath: string): CircleCIEnvironment => ({
      action: 'distribution_release',
      baseBranch: 'master',
      branch: 'master',
      sha1: '784ff35ca',
      changedFiles: [],
      buildNum: '1234',
      buildId: '1234',
      graviteeioVersion: '4.2.0',
      isDryRun: false,
      apimVersionPath,
    });

    it('should read the core the distribution pins, beside the root pom', () => {
      expect(corePin(releaseOf('./src/utils/tests/resources/pinned/pom.xml'))).toStrictEqual('4.1.7');
    });

    it('should refuse a distribution pom that pins nothing', () => {
      expect(() => corePin(releaseOf('./src/utils/tests/resources/unpinned/pom.xml'))).toThrow(
        'corePin - No <apim.core.version> in ./src/utils/tests/resources/unpinned/gravitee-apim-distribution/pom.xml',
      );
    });

    it('should refuse a missing distribution pom', () => {
      expect(() => corePin(releaseOf('./src/utils/tests/resources/pom.xml'))).toThrow(
        'corePin - No file at specified path: ./src/utils/tests/resources/gravitee-apim-distribution/pom.xml',
      );
    });
  });

  describe('bridgeClientTags', () => {
    // Every line that has shipped its first release. The four most recent are the supported ones —
    // a line retires when the fourth release after it ships — so this is what tells a `-latest` tag
    // from a public one.
    const RELEASED = ['4.5.0', '4.6.0', '4.7.0', '4.8.0', '4.9.0', '4.10.0', '4.11.0', '4.12.0', 'core_4.13.0', '4.12.17-hotfix.1'];
    // Every support branch the repository carries. A line's images are published under its branch,
    // and until that branch is cut they are published under master's.
    const BRANCHES = ['4.9.x', '4.10.x', '4.11.x', '4.12.x'];

    // The four lists below are the ones master and the support branches carry by hand today. They
    // are the acceptance of the derivation: reproduce them, or the rule is wrong.
    it('should reproduce the list master carries', () => {
      expect(bridgeClientTags('4.13.0-SNAPSHOT', BRANCHES, RELEASED)).toStrictEqual([
        'master-latest',
        '4.12.x-latest',
        'graviteeio@4.12.0',
        '4.11.x-latest',
        'graviteeio@4.11.0',
        '4.10.x-latest',
        'graviteeio@4.10.0',
      ]);
    });

    it('should reproduce the list 4.12.x carries, where every line is still supported', () => {
      expect(bridgeClientTags('4.12.20-SNAPSHOT', BRANCHES, RELEASED)).toStrictEqual([
        '4.12.x-latest',
        'graviteeio@4.12.0',
        '4.11.x-latest',
        'graviteeio@4.11.0',
        '4.10.x-latest',
        'graviteeio@4.10.0',
        '4.9.x-latest',
        'graviteeio@4.9.0',
      ]);
    });

    // A retired line has no `-latest` image on the registry any more: its last is the public
    // major.minor tag, which stops moving the day the line retires.
    it('should reproduce the list 4.11.x carries, where 4.8 has retired', () => {
      expect(bridgeClientTags('4.11.28-SNAPSHOT', BRANCHES, RELEASED)).toStrictEqual([
        '4.11.x-latest',
        'graviteeio@4.11.0',
        '4.10.x-latest',
        'graviteeio@4.10.0',
        '4.9.x-latest',
        'graviteeio@4.9.0',
        'graviteeio@4.8',
        'graviteeio@4.8.0',
      ]);
    });

    it('should reproduce the list 4.10.x carries, with two retired lines', () => {
      expect(bridgeClientTags('4.10.31-SNAPSHOT', BRANCHES, RELEASED)).toStrictEqual([
        '4.10.x-latest',
        'graviteeio@4.10.0',
        '4.9.x-latest',
        'graviteeio@4.9.0',
        'graviteeio@4.8',
        'graviteeio@4.8.0',
        'graviteeio@4.7',
        'graviteeio@4.7.0',
      ]);
    });

    // The day 4.13.0 ships, 4.9 retires and the list of the line that still tests it follows, without
    // anyone editing anything.
    it('should retire a line as soon as the release that pushes it out has shipped', () => {
      const afterRelease = bridgeClientTags('4.12.20-SNAPSHOT', [...BRANCHES, '4.13.x'], [...RELEASED, '4.13.0']);

      expect(afterRelease).toContain('graviteeio@4.9');
      expect(afterRelease).not.toContain('4.9.x-latest');
    });

    // Between a code freeze and the first release of the line it cut, that line has a branch and no
    // release: its tip is the only image there is to test against.
    it('should take the branch tip alone of a line that has not released yet', () => {
      const duringTheFreeze = bridgeClientTags('4.14.0-SNAPSHOT', [...BRANCHES, '4.13.x'], RELEASED);

      expect(duringTheFreeze).toContain('4.13.x-latest');
      expect(duringTheFreeze).not.toContain('graviteeio@4.13');
      expect(duringTheFreeze).not.toContain('graviteeio@4.13.0');
    });

    // A pull request is not a support branch, and its name says nothing. What decides is the version
    // in its tree and whether that line has been cut yet.
    it('should test a pull request on master against master-latest', () => {
      expect(bridgeClientTags('4.13.0-SNAPSHOT', BRANCHES, RELEASED)[0]).toStrictEqual('master-latest');
    });

    it('should test a pull request targeting 4.12.x against 4.12 images, not master ones', () => {
      const onAPullRequest = bridgeClientTags('4.12.20-SNAPSHOT', BRANCHES, RELEASED);

      expect(onAPullRequest[0]).toStrictEqual('4.12.x-latest');
      expect(onAPullRequest).not.toContain('master-latest');
    });

    // The freeze has cut 4.13.x, so 4.13's images are published under it rather than under master.
    it('should follow a line onto its support branch as soon as the freeze cuts it', () => {
      expect(bridgeClientTags('4.13.0-alpha.1-SNAPSHOT', [...BRANCHES, '4.13.x'], RELEASED)[0]).toStrictEqual('4.13.x-latest');
    });

    it('should refuse to guess across a major', () => {
      expect(() => bridgeClientTags('5.0.0-SNAPSHOT', BRANCHES, RELEASED)).toThrow(
        'bridgeClientTags - 5.0 has fewer than three previous minors in its own major',
      );
    });

    // An empty list is not an answer: every line would look freshly cut, and the matrix would name
    // four branch tips — including retired lines, whose `-latest` image is long gone.
    it('should refuse a tag list with no released line in it', () => {
      expect(() => bridgeClientTags('4.13.0-SNAPSHOT', BRANCHES, ['core_4.13.0', 'not-a-version'])).toThrow(
        'bridgeClientTags - no released line among 2 tags',
      );
    });

    it('should refuse to run without the released tags', () => {
      expect(() => bridgeClientTags('4.13.0-SNAPSHOT', BRANCHES, undefined)).toThrow(
        'bridgeClientTags - the branches and tags are missing',
      );
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

  describe('supportLineOf', function () {
    // A tag-triggered pipeline has no branch, and a job that reads one sends an empty string onward.
    it.each`
      version               | expected
      ${'4.13.0'}           | ${'4.13.x'}
      ${'4.13.4'}           | ${'4.13.x'}
      ${'4.13.0-alpha.1'}   | ${'4.13.x'}
      ${'4.12.17-hotfix.2'} | ${'4.12.x'}
      ${'10.2.0'}           | ${'10.2.x'}
    `('reads $expected from $version', ({ version, expected }) => {
      expect(supportLineOf(version)).toEqual(expected);
    });
  });
});
