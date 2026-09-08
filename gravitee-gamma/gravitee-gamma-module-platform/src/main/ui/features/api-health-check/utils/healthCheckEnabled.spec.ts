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
import { healthCheckEnabled } from './healthCheckEnabled';

describe('healthCheckEnabled', () => {
    it('is false when the API has no endpoint groups', () => {
        expect(healthCheckEnabled({})).toBe(false);
        expect(healthCheckEnabled({ endpointGroups: [] })).toBe(false);
    });

    it('is true when a V4 endpoint group health-check service is enabled', () => {
        expect(
            healthCheckEnabled({
                endpointGroups: [{ services: { healthCheck: { enabled: true } }, endpoints: [] }],
            }),
        ).toBe(true);
    });

    it('is true when a V4 endpoint health-check service is enabled', () => {
        expect(
            healthCheckEnabled({
                endpointGroups: [
                    {
                        services: { healthCheck: { enabled: false } },
                        endpoints: [{ services: { healthCheck: { enabled: true } } }],
                    },
                ],
            }),
        ).toBe(true);
    });

    it('is false when group and endpoint health-check services are missing or disabled', () => {
        expect(
            healthCheckEnabled({
                endpointGroups: [
                    {
                        services: { healthCheck: { enabled: false } },
                        endpoints: [{ services: { healthCheck: { enabled: false } } }, { services: {} }],
                    },
                ],
            }),
        ).toBe(false);
    });
});
