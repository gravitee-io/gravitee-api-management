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

import { apiScoreDetailPath } from './apiScoreDetailPath';

describe('apiScoreDetailPath', () => {
    it('opens API-level Score Details under the APIM module', () => {
        expect(apiScoreDetailPath('/environments/default/platform/api-score', 'api-petstore')).toBe(
            '/environments/default/apim/apis/api-petstore/api-score',
        );
    });

    it('keeps the APIM module path when the platform module has no environment prefix', () => {
        expect(apiScoreDetailPath('/platform/api-score', 'api-1')).toBe('/apim/apis/api-1/api-score');
    });

    it('encodes the API id', () => {
        expect(apiScoreDetailPath('/environments/default/platform/api-score', 'api/1')).toBe(
            '/environments/default/apim/apis/api%2F1/api-score',
        );
    });
});
