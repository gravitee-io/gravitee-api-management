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
import { getDefaultCondition } from './alertDefaults';

describe('getDefaultCondition', () => {
    it('sends Classic STRING MATCHES for node lifecycle even though the UI is info-only', () => {
        expect(getDefaultCondition('NODE_LIFECYCLE@NODE_LIFECYCLE_CHANGED')).toEqual([
            {
                type: 'STRING',
                operator: 'MATCHES',
                property: 'node.event',
                pattern: 'NODE_START|NODE_STOP',
            },
        ]);
    });

    it('sends Classic STRING MATCHES for node health-check even though the UI is info-only', () => {
        expect(getDefaultCondition('NODE_HEALTHCHECK@NODE_HEALTHCHECK')).toEqual([
            {
                type: 'STRING',
                operator: 'MATCHES',
                property: 'node.healthy',
                pattern: '.*',
            },
        ]);
    });

    it('keeps STRING_COMPARE as the health-check endpoint status default', () => {
        expect(getDefaultCondition('ENDPOINT_HEALTH_CHECK@API_HC_ENDPOINT_STATUS_CHANGED')).toEqual([
            { type: 'STRING_COMPARE', property: 'status.old', property2: 'status.new', operator: 'NOT_EQUALS' },
        ]);
    });
});
