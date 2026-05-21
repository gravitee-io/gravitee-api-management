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
import { isGroupAssociatedWithApi, mapApiGroupsToGroupData } from './group-data.utils';

import { fakeGroup } from '../../../entities/management-api-v2';

describe('group-data.utils', () => {
  const group = fakeGroup({ id: 'group-uuid', name: 'my-group' });

  describe('mapApiGroupsToGroupData', () => {
    it('should resolve group id references', () => {
      expect(mapApiGroupsToGroupData(['group-uuid'], [group])).toEqual([{ id: 'group-uuid', name: 'my-group', isVisible: true }]);
    });

    it('should resolve group name references (V2 APIs)', () => {
      expect(mapApiGroupsToGroupData(['my-group'], [group])).toEqual([{ id: 'group-uuid', name: 'my-group', isVisible: true }]);
    });

    it('should skip unknown references', () => {
      expect(mapApiGroupsToGroupData(['unknown'], [group])).toEqual([]);
    });
  });

  describe('isGroupAssociatedWithApi', () => {
    it('should match by id or name', () => {
      expect(isGroupAssociatedWithApi(['group-uuid'], group)).toBe(true);
      expect(isGroupAssociatedWithApi(['my-group'], group)).toBe(true);
      expect(isGroupAssociatedWithApi(['other'], group)).toBe(false);
    });
  });
});
