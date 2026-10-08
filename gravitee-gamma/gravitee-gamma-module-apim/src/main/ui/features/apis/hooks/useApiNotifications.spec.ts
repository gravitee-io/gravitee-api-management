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
import { groupHooksByCategory } from './useApiNotifications';
import type { ApiHook } from '../types/notification';

const hook = (id: string, category: string): ApiHook => ({ id, label: id, description: '', scope: 'API', category });

describe('groupHooksByCategory', () => {
    it('groups hooks by category, preserving backend order', () => {
        const groups = groupHooksByCategory([
            hook('API_STARTED', 'LIFECYCLE'),
            hook('SUBSCRIPTION_NEW', 'SUBSCRIPTION'),
            hook('API_STOPPED', 'LIFECYCLE'),
        ]);

        expect(groups.map(g => g.name)).toEqual(['LIFECYCLE', 'SUBSCRIPTION']);
        expect(groups[0].hooks.map(h => h.id)).toEqual(['API_STARTED', 'API_STOPPED']);
    });

    it('leaves out the Review, Support and Rating hooks, which only apply to the classic Developer Portal', () => {
        const groups = groupHooksByCategory([
            hook('API_STARTED', 'LIFECYCLE'),
            hook('ASK_FOR_REVIEW', 'REVIEW'),
            hook('REVIEW_OK', 'REVIEW'),
            hook('REQUEST_FOR_CHANGES', 'REVIEW'),
            hook('NEW_SUPPORT_TICKET', 'SUPPORT'),
            hook('NEW_RATING', 'RATING'),
            hook('NEW_RATING_ANSWER', 'RATING'),
        ]);

        expect(groups.map(g => g.name)).toEqual(['LIFECYCLE']);
    });
});
