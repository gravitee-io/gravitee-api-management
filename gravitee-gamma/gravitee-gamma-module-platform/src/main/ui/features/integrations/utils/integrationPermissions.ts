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

export const ENVIRONMENT_INTEGRATION_CREATE_PERMISSION = 'environment-integration-c' as const;
export const INTEGRATION_DEFINITION_READ_PERMISSION = 'integration-definition-r' as const;
export const INTEGRATION_DEFINITION_UPDATE_PERMISSION = 'integration-definition-u' as const;
export const INTEGRATION_DEFINITION_DELETE_PERMISSION = 'integration-definition-d' as const;
export const ENVIRONMENT_API_DELETE_PERMISSION = 'environment-api-d' as const;
export const INTEGRATION_CONFIGURATION_PERMISSIONS = [INTEGRATION_DEFINITION_UPDATE_PERMISSION, INTEGRATION_DEFINITION_DELETE_PERMISSION];
