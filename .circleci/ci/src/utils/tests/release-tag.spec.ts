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
import { coreVersionFromTag, distributionVersionFromTag } from '../release-tag';

describe('coreVersionFromTag', () => {
  it.each(['core_4.13.0', 'core_4.13.12', 'core_10.0.0'])('reads the version of a final release tag: %s', (tag) => {
    expect(coreVersionFromTag(tag)).toEqual(tag.substring('core_'.length));
  });

  it.each(['core_4.13.0-alpha.1', 'core_4.13.0-beta.12', 'core_4.13.0-rc.1', 'core_4.12.17-hotfix.2'])(
    'reads the version of a qualified tag: %s',
    (tag) => {
      expect(coreVersionFromTag(tag)).toEqual(tag.substring('core_'.length));
    },
  );

  it('ignores the product tag, which stays bare', () => {
    expect(coreVersionFromTag('4.13.0')).toBeUndefined();
  });

  it.each(['', 'master', 'core_', 'core_4.13', 'core_4.13.0.1', 'core_4.13.0-alpha', 'xcore_4.13.0', 'core_4.13.0-ALPHA.1'])(
    'ignores what is not a core release tag: %s',
    (tag) => {
      expect(coreVersionFromTag(tag)).toBeUndefined();
    },
  );
});

describe('distributionVersionFromTag', () => {
  it.each`
    tag                   | expected
    ${'4.13.0'}           | ${'4.13.0'}
    ${'4.13.0-alpha.1'}   | ${'4.13.0-alpha.1'}
    ${'4.12.17-hotfix.2'} | ${'4.12.17-hotfix.2'}
  `('reads $expected from $tag', ({ tag, expected }) => {
    expect(distributionVersionFromTag(tag)).toEqual(expected);
  });

  // The two lanes share the repository, so each pattern has to leave the other's tags alone.
  it.each`
    tag
    ${'core_4.13.0'}
    ${''}
    ${'v4.13.0'}
    ${'4.13'}
    ${'nightly'}
  `('ignores $tag', ({ tag }) => {
    expect(distributionVersionFromTag(tag)).toBeUndefined();
  });
});
