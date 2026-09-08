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
jest.mock('@gravitee/gamma-modules-sdk/routing', () => jest.requireActual('../../users/testing/buildModuleNavPathForTests'));

import { healthCheckDashboardPath } from './healthCheckDashboardPath';

describe('healthCheckDashboardPath', () => {
    it('opens the API Health Check Dashboard under endpoints, not Overview', () => {
        expect(healthCheckDashboardPath('/environments/default/platform/api-health-check', 'api-inventory')).toBe(
            '/environments/default/apim/apis/api-inventory/endpoints/health-check-dashboard',
        );
    });

    it('keeps the APIM module path when the platform module has no environment prefix', () => {
        expect(healthCheckDashboardPath('/platform/api-health-check', 'api-1')).toBe('/apim/apis/api-1/endpoints/health-check-dashboard');
    });

    it('encodes the API id in the dashboard path the same way availability requests do', () => {
        expect(healthCheckDashboardPath('/environments/default/platform/api-health-check', 'api/1')).toBe(
            '/environments/default/apim/apis/api%2F1/endpoints/health-check-dashboard',
        );
    });
});
