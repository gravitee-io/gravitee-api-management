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
jest.mock('@gravitee/gamma-modules-sdk/routing', () => jest.requireActual('../../../testing/buildModuleNavPathForTests'));

import { buildIntegrationOverviewPath } from './integrationOverviewPath';

describe('buildIntegrationOverviewPath', () => {
    it('opens the integration overview under the platform module, copying the environment segment from the current path', () => {
        expect(buildIntegrationOverviewPath('/environments/DEFAULT/apim/apis/api-1/general', 'int-1')).toBe(
            '/environments/DEFAULT/platform/integrations/int-1',
        );
    });

    it('encodes the integration id', () => {
        expect(buildIntegrationOverviewPath('/environments/DEFAULT/apim/apis/api-1/general', 'int/1')).toBe(
            '/environments/DEFAULT/platform/integrations/int%2F1',
        );
    });
});
