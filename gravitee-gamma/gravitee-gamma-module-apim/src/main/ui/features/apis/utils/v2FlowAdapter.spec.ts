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
    mapFlowModeToExecution,
    mapFlowsV2ToV4,
    mapFlowV2ToV4,
    V2_DEFAULT_ENDPOINTS_INFO,
    V2_DEFAULT_ENTRYPOINTS_INFO,
} from './v2FlowAdapter';
import type { FlowV2, StepV2 } from '../types/policyStudio';

const STEP_PRE: StepV2 = {
    policy: 'rate-limit',
    name: 'Rate Limit',
    enabled: true,
    configuration: { rate: { limit: 10 } },
};

const STEP_POST: StepV2 = {
    policy: 'transform-headers',
    name: 'Add CORS',
    description: 'Adds CORS headers',
    enabled: false,
    condition: '{#request.headers["origin"] != null}',
    configuration: { addHeaders: [{ name: 'Access-Control-Allow-Origin', value: '*' }] },
};

describe('mapFlowV2ToV4', () => {
    it('converts a flow with pathOperator and methods to an HTTP selector', () => {
        const flow: FlowV2 = {
            id: 'flow-1',
            name: 'GET users',
            pathOperator: { path: '/users', operator: 'STARTS_WITH' },
            methods: ['GET', 'HEAD'],
            pre: [STEP_PRE],
            post: [STEP_POST],
            enabled: true,
        };

        const result = mapFlowV2ToV4(flow);

        expect(result.id).toBe('flow-1');
        expect(result.name).toBe('GET users');
        expect(result.enabled).toBe(true);
        expect(result.selectors).toHaveLength(1);
        expect(result.selectors![0]).toMatchObject({
            type: 'HTTP',
            path: '/users',
            pathOperator: 'STARTS_WITH',
            methods: ['GET', 'HEAD'],
        });
        expect(result.request).toHaveLength(1);
        expect(result.request![0]).toMatchObject({ policy: 'rate-limit', name: 'Rate Limit', enabled: true });
        expect(result.response).toHaveLength(1);
        expect(result.response![0]).toMatchObject({ policy: 'transform-headers', enabled: false });
    });

    it('converts a flow with a condition to a CONDITION selector', () => {
        const flow: FlowV2 = {
            id: 'flow-2',
            condition: '{#request.headers["x-custom"] == "true"}',
            pre: [STEP_PRE],
        };

        const result = mapFlowV2ToV4(flow);

        expect(result.selectors).toHaveLength(1);
        expect(result.selectors![0]).toMatchObject({
            type: 'CONDITION',
            condition: '{#request.headers["x-custom"] == "true"}',
        });
    });

    it('produces both HTTP and CONDITION selectors when flow has pathOperator and condition', () => {
        const flow: FlowV2 = {
            id: 'flow-3',
            pathOperator: { path: '/admin', operator: 'EQUALS' },
            methods: ['POST'],
            condition: '{#request.headers["x-admin"] != null}',
            pre: [STEP_PRE],
        };

        const result = mapFlowV2ToV4(flow);

        expect(result.selectors).toHaveLength(2);
        expect(result.selectors![0].type).toBe('HTTP');
        expect(result.selectors![1].type).toBe('CONDITION');
    });

    it('handles a flow with no pre/post steps', () => {
        const flow: FlowV2 = {
            id: 'flow-4',
            pathOperator: { path: '/', operator: 'STARTS_WITH' },
        };

        const result = mapFlowV2ToV4(flow);

        expect(result.request).toBeUndefined();
        expect(result.response).toBeUndefined();
    });

    it('defaults enabled to true when not specified', () => {
        const flow: FlowV2 = { id: 'flow-5' };

        const result = mapFlowV2ToV4(flow);

        expect(result.enabled).toBe(true);
    });

    it('respects explicit enabled=false', () => {
        const flow: FlowV2 = { id: 'flow-6', enabled: false };

        const result = mapFlowV2ToV4(flow);

        expect(result.enabled).toBe(false);
    });

    it('returns undefined selectors when no pathOperator and no condition', () => {
        const flow: FlowV2 = { id: 'flow-7', pre: [STEP_PRE] };

        const result = mapFlowV2ToV4(flow);

        expect(result.selectors).toBeUndefined();
    });
});

describe('mapFlowsV2ToV4', () => {
    it('maps an array of V2 flows', () => {
        const flows: FlowV2[] = [
            { id: 'a', pathOperator: { path: '/a', operator: 'EQUALS' } },
            { id: 'b', condition: '{#true}' },
        ];

        const result = mapFlowsV2ToV4(flows);

        expect(result).toHaveLength(2);
        expect(result[0].id).toBe('a');
        expect(result[1].id).toBe('b');
    });

    it('returns empty array for empty input', () => {
        expect(mapFlowsV2ToV4([])).toEqual([]);
    });
});

describe('mapFlowModeToExecution', () => {
    it('maps DEFAULT mode', () => {
        expect(mapFlowModeToExecution('DEFAULT')).toEqual({ mode: 'DEFAULT' });
    });

    it('maps BEST_MATCH mode', () => {
        expect(mapFlowModeToExecution('BEST_MATCH')).toEqual({ mode: 'BEST_MATCH' });
    });

    it('defaults to DEFAULT when undefined', () => {
        expect(mapFlowModeToExecution(undefined)).toEqual({ mode: 'DEFAULT' });
    });
});

describe('V2 default connector info constants', () => {
    it('V2_DEFAULT_ENTRYPOINTS_INFO has http-proxy with REQUEST_RESPONSE', () => {
        expect(V2_DEFAULT_ENTRYPOINTS_INFO).toHaveLength(1);
        expect(V2_DEFAULT_ENTRYPOINTS_INFO[0].type).toBe('http-proxy');
        expect(V2_DEFAULT_ENTRYPOINTS_INFO[0].supportedModes).toContain('REQUEST_RESPONSE');
    });

    it('V2_DEFAULT_ENDPOINTS_INFO has http-proxy with REQUEST_RESPONSE', () => {
        expect(V2_DEFAULT_ENDPOINTS_INFO).toHaveLength(1);
        expect(V2_DEFAULT_ENDPOINTS_INFO[0].type).toBe('http-proxy');
        expect(V2_DEFAULT_ENDPOINTS_INFO[0].supportedModes).toContain('REQUEST_RESPONSE');
    });
});
