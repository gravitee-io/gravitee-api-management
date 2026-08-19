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

export const ORGANIZATION_AUDIT_READ_PERMISSION = 'organization-audit-r' as const;
export const ENVIRONMENT_AUDIT_READ_PERMISSION = 'environment-audit-r' as const;

// Scoped separately, matching Classic: `organization-audit-r` gates the Organization Audit page and
// `environment-audit-r` gates the Environment one. Granting only one must not surface the other.
export const ORGANIZATION_AUDIT_READ_PERMISSIONS = [ORGANIZATION_AUDIT_READ_PERMISSION] as const;
export const ENVIRONMENT_AUDIT_READ_PERMISSIONS = [ENVIRONMENT_AUDIT_READ_PERMISSION] as const;
