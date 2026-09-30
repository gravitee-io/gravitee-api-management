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
    defineObservabilityFeatures,
    resolveLogEntityLink,
    type LogEntity,
    type LogEntityRef,
    type ObservabilityFeaturesConfig,
} from '@gravitee/gamma-lib-observability';

import '../observability';

jest.mock('@gravitee/gamma-lib-observability', () => {
    const actual = jest.requireActual('@gravitee/gamma-lib-observability');
    return { ...actual, defineObservabilityFeatures: jest.fn(actual.defineObservabilityFeatures) };
});

const ENVIRONMENT_ROOT = '/environments/DEFAULT';
const LOG_ROW: LogEntityRef = { apiType: 'HTTP_PROXY', apiId: 'api-1', planId: 'plan-1', applicationId: 'app-1' };

function linkOf(entity: LogEntity): string | undefined {
    const [config] = jest.mocked(defineObservabilityFeatures).mock.calls[0] as [ObservabilityFeaturesConfig];
    return resolveLogEntityLink(ENVIRONMENT_ROOT, entity, LOG_ROW, config.features.logs?.entityLinks);
}

describe('observability log entity links', () => {
    it('links an API name to its page in this module', () => {
        expect(linkOf('api')).toBe('/environments/DEFAULT/apim/apis/api-1');
    });

    it('links a plan name to the plans tab of its API', () => {
        expect(linkOf('plan')).toBe('/environments/DEFAULT/apim/apis/api-1/plans');
    });

    it('links an application name to its page in the Platform module', () => {
        expect(linkOf('application')).toBe('/environments/DEFAULT/platform/applications/app-1');
    });
});
