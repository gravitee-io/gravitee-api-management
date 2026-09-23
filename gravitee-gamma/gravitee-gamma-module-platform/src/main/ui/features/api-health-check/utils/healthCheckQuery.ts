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

export const HEALTH_CHECK_FILTER_QUERY = 'has_health_check:true';

/**
 * Classic's health check search scopes by definition version, not by api type
 * (`home-api-health-check.component.ts` sends `definitionVersions: ['V2', 'V4']`). Gamma sends the same
 * field so both consoles hit the endpoint the same way; the value stays V4 because Gamma's APIM module
 * only has a V4 health check dashboard to link a row to.
 */
export const HEALTH_CHECK_DEFINITION_VERSIONS = ['V4'] as const;
