/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {
    cleanseNotificationGroups,
    mapApiGroupsToNotificationOptions,
    selectedGroupsWithPrimaryOwner,
    withPrimaryOwnerOption,
} from './notificationGroups';
import type { Group } from '../types/members.types';

const GROUP: Group = { id: 'group-uuid', name: 'my-group' };

describe('mapApiGroupsToNotificationOptions', () => {
    it('maps an API group id to the matching group option', () => {
        expect(mapApiGroupsToNotificationOptions(['group-uuid'], [GROUP])).toEqual([{ id: 'group-uuid', name: 'my-group' }]);
    });

    it('maps an API group name (V2) to the matching group option', () => {
        expect(mapApiGroupsToNotificationOptions(['my-group'], [GROUP])).toEqual([{ id: 'group-uuid', name: 'my-group' }]);
    });

    it('skips unknown group references', () => {
        expect(mapApiGroupsToNotificationOptions(['unknown'], [GROUP])).toEqual([]);
    });
});

describe('withPrimaryOwnerOption', () => {
    it('prepends a Primary Owner option when missing', () => {
        expect(withPrimaryOwnerOption([{ id: 'g1', name: 'G1' }], 'po-1')).toEqual([
            { id: 'po-1', name: 'Primary Owner' },
            { id: 'g1', name: 'G1' },
        ]);
    });

    it('does not duplicate when the primary owner is already present', () => {
        expect(withPrimaryOwnerOption([{ id: 'po-1', name: 'Owners' }], 'po-1')).toEqual([{ id: 'po-1', name: 'Owners' }]);
    });
});

describe('selectedGroupsWithPrimaryOwner', () => {
    it('adds the primary owner when absent from notification groups', () => {
        expect(selectedGroupsWithPrimaryOwner(['g1'], 'po-1')).toEqual(['g1', 'po-1']);
    });

    it('keeps existing primary owner selection', () => {
        expect(selectedGroupsWithPrimaryOwner(['po-1', 'g1'], 'po-1')).toEqual(['po-1', 'g1']);
    });
});

describe('cleanseNotificationGroups', () => {
    it('strips the primary owner id before save', () => {
        expect(cleanseNotificationGroups(['g1', 'po-1'], 'po-1')).toEqual(['g1']);
    });
});
