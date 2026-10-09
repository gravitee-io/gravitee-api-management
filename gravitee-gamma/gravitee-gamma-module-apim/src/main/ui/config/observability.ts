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
import { defineObservabilityFeatures, type DashboardCapabilities } from '@gravitee/gamma-lib-observability';

import { observabilityTemplates } from './templates';

export const PERMISSIVE_CAPABILITIES: DashboardCapabilities = {
    'observability.dashboards.read': true,
    'observability.dashboards.write': true,
    'observability.logs.read': true,
    'observability.traces.read': true,
};

export const observability = defineObservabilityFeatures({
    // The plugin id (plugin.properties): scopes custom dashboards and tracing filters to APIM.
    module: 'apim',
    scopeApiTypes: ['HTTP_PROXY'],
    features: {
        dashboards: { enabled: true, templates: observabilityTemplates },
        logs: {
            enabled: true,
            entityLinks: {
                api: '/apim/apis/:apiId',
                plan: '/apim/apis/:apiId/plans',
                application: '/platform/applications/:applicationId',
            },
        },
        tracing: { enabled: true },
    },
    nav: { label: 'Observability' },
});
