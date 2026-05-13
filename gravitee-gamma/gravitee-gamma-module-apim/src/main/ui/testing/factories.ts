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

export const TEST_CONFIG = {
    gammaBaseURL: 'http://api.test/gamma',
    managementBaseURL: 'http://api.test/management',
    environmentId: 'DEFAULT',
    organizationId: 'DEFAULT',
} as const;

/** Base URL for APIM V2 environment-scoped endpoints. */
export const TEST_V2_BASE = `${TEST_CONFIG.managementBaseURL}/v2/environments/${TEST_CONFIG.environmentId}`;
