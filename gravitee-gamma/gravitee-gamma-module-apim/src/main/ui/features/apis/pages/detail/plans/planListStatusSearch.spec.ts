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
import { isPlanStatus, planListSearchForStatus, planStatusFromSearchParam } from './planListStatusSearch';
import type { PlanStatus } from '../../../types/plan';

describe('planStatusFromSearchParam', () => {
    it.each<PlanStatus>(['STAGING', 'PUBLISHED', 'DEPRECATED', 'CLOSED'])('reads back the %s bucket the URL names', status => {
        expect(planStatusFromSearchParam(status)).toBe(status);
    });

    /** Classic Console opens on Published (`plan-list.component.ts`, `fs?.selectedStatus ?? 'PUBLISHED'`). */
    it.each([
        ['no param at all', null],
        ['a param the route did not set', undefined],
        ['an empty param', ''],
        ['a status in the wrong case', 'staging'],
        ['a status that does not exist', 'ARCHIVED'],
    ])('falls back to Published for %s', (_case, value) => {
        expect(planStatusFromSearchParam(value)).toBe('PUBLISHED');
    });
});

describe('isPlanStatus', () => {
    it('accepts every status the list can show', () => {
        expect(['STAGING', 'PUBLISHED', 'DEPRECATED', 'CLOSED'].every(isPlanStatus)).toBe(true);
    });

    it('rejects anything else, including a near miss', () => {
        expect(['Staging', 'published ', 'ARCHIVED', '', null, undefined].some(isPlanStatus)).toBe(false);
    });
});

describe('planListSearchForStatus', () => {
    it('names the bucket the list should open on', () => {
        expect(planListSearchForStatus('STAGING')).toBe('?status=STAGING');
    });

    /** What it writes has to be what the list reads back, or a redirect lands on the wrong bucket. */
    it.each<PlanStatus>(['STAGING', 'PUBLISHED', 'DEPRECATED', 'CLOSED'])('round-trips %s through the list', status => {
        const search = new URLSearchParams(planListSearchForStatus(status));

        expect(planStatusFromSearchParam(search.get('status'))).toBe(status);
    });
});
