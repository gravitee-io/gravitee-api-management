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
import { CircleCIEnvironment } from '../../pipelines';
import { Variant } from '../../config';
import { parse } from '../../utils';
import { dockerTagsArgument } from '../job-build-docker-image';

describe('dockerTagsArgument', () => {
  const environmentOf = (graviteeioVersion: string, overrides: Partial<CircleCIEnvironment> = {}): CircleCIEnvironment => ({
    action: 'distribution_release',
    baseBranch: '4.2.x',
    branch: '',
    buildNum: '1234',
    buildId: '1234',
    sha1: '784ff35ca',
    isDryRun: false,
    graviteeioVersion,
    changedFiles: [],
    apimVersionPath: '',
    ...overrides,
  });

  const tagsOf = (version: string, variant?: Variant, isProd = true, overrides: Partial<CircleCIEnvironment> = {}) =>
    dockerTagsArgument(environmentOf(version, overrides), parse(version), isProd, variant, 'apim-gateway');

  describe('alpine backend images', () => {
    it('should also tag a release with -alpine', () => {
      expect(tagsOf('4.2.3', 'alpine')).toStrictEqual([
        'graviteeio/apim-gateway:4.2.3',
        'graviteeio/apim-gateway:4.2',
        'graviteeio/apim-gateway:4.2.3-alpine',
        'graviteeio/apim-gateway:4.2-alpine',
      ]);
    });

    it('should also tag the latest release with -alpine', () => {
      expect(tagsOf('4.2.3', 'alpine', true, { dockerTagAsLatest: true })).toStrictEqual([
        'graviteeio/apim-gateway:4.2.3',
        'graviteeio/apim-gateway:4.2',
        'graviteeio/apim-gateway:4',
        'graviteeio/apim-gateway:latest',
        'graviteeio/apim-gateway:4.2.3-alpine',
        'graviteeio/apim-gateway:4.2-alpine',
        'graviteeio/apim-gateway:4-alpine',
        'graviteeio/apim-gateway:latest-alpine',
      ]);
    });

    it.each`
      version             | expected
      ${'4.2.0-alpha.1'}  | ${['4.2.0-alpha.1', '4.2.0-alpha', '4.2.0-alpha.1-alpine', '4.2.0-alpha-alpine']}
      ${'4.2.0-hotfix.1'} | ${['4.2.0-hotfix.1', '4.2.0-hotfix', '4.2.0-hotfix.1-alpine', '4.2.0-hotfix-alpine']}
    `('should also tag the qualified release $version with -alpine', ({ version, expected }) => {
      expect(tagsOf(version, 'alpine')).toStrictEqual(expected.map((tag: string) => `graviteeio/apim-gateway:${tag}`));
    });

    it('should not add -alpine tags to the images of a branch build', () => {
      expect(tagsOf('4.2.3', 'alpine', false, { branch: 'master', sha1: 'abc' })).toStrictEqual([
        'graviteeio.azurecr.io/apim-gateway:master-latest',
        'graviteeio.azurecr.io/apim-gateway:master-abc',
      ]);
    });
  });

  it.each<[Variant | undefined, string[]]>([
    [undefined, ['4.2.3', '4.2']],
    ['debian', ['4.2.3-debian', '4.2-debian']],
    ['chainguard', ['4.2.3-chainguard', '4.2-chainguard']],
  ])('should keep the tags of variant %s unchanged', (variant, expected) => {
    expect(tagsOf('4.2.3', variant)).toStrictEqual(expected.map((tag) => `graviteeio/apim-gateway:${tag}`));
  });
});
