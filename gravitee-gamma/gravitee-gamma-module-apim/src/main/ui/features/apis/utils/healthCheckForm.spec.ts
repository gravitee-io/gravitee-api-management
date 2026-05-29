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
    buildGroupHealthCheckService,
    getEndpointHealthCheckBadge,
    healthCheckFromEndpoint,
    healthCheckFromGroup,
    validateHealthCheckForm,
} from './healthCheckForm';

describe('healthCheckForm', () => {
    it('maps group health-check from API', () => {
        const form = healthCheckFromGroup({
            enabled: true,
            type: 'http-health-check',
            configuration: { schedule: '0 0 * * * *', target: '/hc' },
        });
        expect(form.enabled).toBe(true);
        expect(form.configuration.target).toBe('/hc');
    });

    it('detects endpoint inherit from overrideConfiguration', () => {
        const inherited = healthCheckFromEndpoint(undefined, { enabled: true, configuration: { target: '/g' } });
        expect(inherited.inherit).toBe(true);

        const overridden = healthCheckFromEndpoint(
            { enabled: false, overrideConfiguration: true, configuration: { target: '/e' } },
            { enabled: true, configuration: { target: '/g' } },
        );
        expect(overridden.inherit).toBe(false);
        expect(overridden.configuration.target).toBe('/e');
    });

    it('validates required fields when enabled', () => {
        const errors = validateHealthCheckForm({
            enabled: true,
            configuration: {
                schedule: 'bad',
                method: 'GET',
                target: '',
                overrideEndpointPath: true,
                headers: [],
                assertion: '',
                successThreshold: 0,
                failureThreshold: 0,
            },
        });
        expect(errors.target).toBeDefined();
        expect(errors.schedule).toBeDefined();
        expect(errors.assertion).toBeDefined();
    });

    it('clears health-check service when disabled on group', () => {
        expect(
            buildGroupHealthCheckService({ enabled: false, configuration: healthCheckFromGroup(undefined).configuration }),
        ).toBeUndefined();
    });

    it('returns health-check list badge when group or endpoint has health check enabled', () => {
        expect(
            getEndpointHealthCheckBadge({ services: { healthCheck: { enabled: true } } }, { type: 'http-proxy', services: {} })?.tooltip,
        ).toContain('inherited');
        expect(
            getEndpointHealthCheckBadge({ services: {} }, { type: 'http-proxy', services: { healthCheck: { enabled: true } } })?.tooltip,
        ).toContain('endpoint configuration');
        expect(getEndpointHealthCheckBadge({ services: {} }, { type: 'kafka', services: {} })).toBeNull();
    });
});
