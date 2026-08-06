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
jest.mock('@gravitee/gamma-modules-sdk/routing', () => jest.requireActual('../testing/buildModuleNavPathForTests'));

import {
    buildInheritedApiDetailPath,
    buildInheritedApiProductDetailPath,
    buildInheritedApplicationDetailPath,
    primaryEnvironmentSegment,
    resolveEnvironmentSegment,
} from './crossModuleResourcePath';

const ENVIRONMENTS = [
    { id: 'DEFAULT', name: 'Default environment', hrids: ['default'] },
    { id: 'ENV_A', name: 'Environment A', hrids: ['env-a'] },
];

const USER_DETAIL_PATH = '/environments/default/platform/users/user-1';

describe('crossModuleResourcePath', () => {
    it('resolves environment URLs from technical ids to primary hrids', () => {
        expect(primaryEnvironmentSegment({ id: 'DEFAULT', hrids: ['default'] })).toBe('default');
        expect(resolveEnvironmentSegment('DEFAULT', ENVIRONMENTS)).toBe('default');
    });

    it('navigates from user detail to an inherited API in the same environment', () => {
        expect(buildInheritedApiDetailPath('api-1', 'DEFAULT', ENVIRONMENTS, USER_DETAIL_PATH)).toBe(
            '/environments/default/apim/apis/api-1',
        );
    });

    it('navigates from user detail to an inherited API product in the same environment', () => {
        expect(buildInheritedApiProductDetailPath('product-1', 'DEFAULT', ENVIRONMENTS, USER_DETAIL_PATH)).toBe(
            '/environments/default/apim/api-products/product-1/configuration/general',
        );
    });

    it('navigates from user detail to an inherited application in the same environment', () => {
        expect(buildInheritedApplicationDetailPath('app-1', 'DEFAULT', ENVIRONMENTS, USER_DETAIL_PATH)).toBe(
            '/environments/default/platform/applications/app-1',
        );
    });

    it('navigates to an inherited API in a different environment tab', () => {
        expect(buildInheritedApiDetailPath('api-9', 'ENV_A', ENVIRONMENTS, USER_DETAIL_PATH)).toBe('/environments/env-a/apim/apis/api-9');
    });

    it('navigates to inherited resources when the platform module runs without an environment shell prefix', () => {
        expect(buildInheritedApiDetailPath('api-1', 'DEFAULT', ENVIRONMENTS, '/platform/users/user-1')).toBe('/apim/apis/api-1');
        expect(buildInheritedApplicationDetailPath('app-1', 'DEFAULT', ENVIRONMENTS, '/platform/users/user-1')).toBe(
            '/platform/applications/app-1',
        );
    });
});
