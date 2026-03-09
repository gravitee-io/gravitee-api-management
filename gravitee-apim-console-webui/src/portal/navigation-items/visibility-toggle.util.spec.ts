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
import { getPublicVisibilityDisabledTooltip, isPublicVisibilityDisabled } from './visibility-toggle.util';

import { fakePortalNavigationApi, fakePortalNavigationFolder } from '../../entities/management-api-v2';

describe('visibility-toggle.util', () => {
  describe('isPublicVisibilityDisabled', () => {
    it('returns false when parent item is not provided', () => {
      expect(isPublicVisibilityDisabled(undefined)).toBe(false);
    });

    it('returns false when parent item visibility is PUBLIC', () => {
      const parent = fakePortalNavigationFolder({ visibility: 'PUBLIC' });
      expect(isPublicVisibilityDisabled(parent)).toBe(false);
    });

    it('returns true when parent item visibility is PRIVATE', () => {
      const parent = fakePortalNavigationFolder({ visibility: 'PRIVATE' });
      expect(isPublicVisibilityDisabled(parent)).toBe(true);
    });
  });

  describe('getPublicVisibilityDisabledTooltip', () => {
    it('returns empty string when parent item is not provided', () => {
      expect(getPublicVisibilityDisabledTooltip(undefined)).toBe('');
    });

    it('returns empty string when parent item visibility is PUBLIC', () => {
      const parent = fakePortalNavigationFolder({ visibility: 'PUBLIC' });
      expect(getPublicVisibilityDisabledTooltip(parent)).toBe('');
    });

    it('returns tooltip message with lower-cased parent type when parent is private', () => {
      const parent = fakePortalNavigationApi({ visibility: 'PRIVATE' });
      expect(getPublicVisibilityDisabledTooltip(parent)).toBe('This navigation item is in api requiring authentication');
    });
  });
});
