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
import { toFlowExecution, toFlowMode, toPlatformFlow, toStudioFlow } from './platformFlowAdapter';
import type { PlatformFlow } from '../types/platformPolicies';

const STORED_FLOW: PlatformFlow = {
    id: 'flow-1',
    name: 'Partner traffic',
    enabled: true,
    'path-operator': { path: '/partners', operator: 'STARTS_WITH' },
    methods: ['GET', 'POST'],
    condition: '{#request.headers.get("X-Partner") != null}',
    pre: [{ policy: 'rate-limit', name: 'Rate limit', enabled: true, configuration: { limit: 10 } }],
    post: [{ policy: 'transform-headers', name: 'Correlation header', enabled: true, configuration: {} }],
    consumers: [
        { consumerType: 'TAG', consumerId: 'tag-eu' },
        { consumerType: 'TAG', consumerId: 'tag-us' },
    ],
};

describe('platformFlowAdapter', () => {
    describe('toStudioFlow', () => {
        it('maps the path operator and the condition to selectors', () => {
            expect(toStudioFlow(STORED_FLOW).selectors).toEqual([
                { type: 'HTTP', path: '/partners', pathOperator: 'STARTS_WITH', methods: ['GET', 'POST'] },
                { type: 'CONDITION', condition: '{#request.headers.get("X-Partner") != null}' },
            ]);
        });

        it('maps pre and post steps to the request and response phases', () => {
            const flow = toStudioFlow(STORED_FLOW);
            expect(flow.request).toEqual([
                {
                    policy: 'rate-limit',
                    name: 'Rate limit',
                    enabled: true,
                    configuration: { limit: 10 },
                    description: undefined,
                    condition: undefined,
                },
            ]);
            expect(flow.response?.[0]?.policy).toBe('transform-headers');
        });

        it('exposes tag consumers as flow tags', () => {
            expect(toStudioFlow(STORED_FLOW).tags).toEqual(['tag-eu', 'tag-us']);
        });

        it('treats a flow with no enabled field as enabled', () => {
            expect(toStudioFlow({ name: 'Legacy' }).enabled).toBe(true);
        });
    });

    describe('toPlatformFlow', () => {
        it('restores the stored shape of a flow it read', () => {
            expect(toPlatformFlow(toStudioFlow(STORED_FLOW))).toEqual(STORED_FLOW);
        });

        it('writes tags back as TAG consumers', () => {
            expect(toPlatformFlow({ name: 'Tagged', tags: ['tag-eu'] }).consumers).toEqual([{ consumerType: 'TAG', consumerId: 'tag-eu' }]);
        });

        it('falls back to a root path operator when the flow has no HTTP selector', () => {
            expect(toPlatformFlow({ name: 'No selector' })['path-operator']).toEqual({ path: '/', operator: 'STARTS_WITH' });
        });
    });

    describe('flow mode', () => {
        it('defaults an organization without a flow mode to DEFAULT', () => {
            expect(toFlowExecution(undefined)).toEqual({ mode: 'DEFAULT' });
        });

        it('round-trips BEST_MATCH', () => {
            expect(toFlowMode(toFlowExecution('BEST_MATCH'))).toBe('BEST_MATCH');
        });
    });
});
