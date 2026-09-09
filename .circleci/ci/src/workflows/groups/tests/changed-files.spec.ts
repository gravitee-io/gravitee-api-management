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
import { assemblesPinnedCore } from '../changed-files';

describe('assemblesPinnedCore', () => {
  // What production actually hands over: `keepFirstPathItem` has already reduced every path to its
  // first segment, so these lists never contain a separator. The nested forms below are the fixture
  // convention, kept because the predicate is anchored and has to stay so.
  it('assembles the pin when only the distribution changed', () => {
    expect(assemblesPinnedCore(['gravitee-apim-distribution'])).toBe(true);
  });

  it('accepts a nested path too, which the reduction upstream never produces', () => {
    expect(assemblesPinnedCore(['gravitee-apim-distribution/pom.xml'])).toBe(true);
  });

  it('assembles the branch core as soon as anything outside the distribution changed', () => {
    expect(assemblesPinnedCore(['gravitee-apim-distribution/pom.xml', 'gravitee-apim-gateway/pom.xml'])).toBe(false);
  });

  it('assembles the branch core when the changed files are unknown', () => {
    // A master or support-branch build: index.ts hands over an empty list rather than a diff, and
    // `every` on an empty array would otherwise say the change is distribution-only.
    expect(assemblesPinnedCore([])).toBe(false);
  });

  it('does not mistake a sibling path for the distribution', () => {
    expect(assemblesPinnedCore(['gravitee-apim-distribution-something/pom.xml'])).toBe(false);
  });
});
