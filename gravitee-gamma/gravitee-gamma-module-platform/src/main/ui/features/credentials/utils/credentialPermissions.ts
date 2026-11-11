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
export const ENVIRONMENT_CREDENTIAL_PERMISSION_PREFIX = 'environment-credential-' as const;
export const ENVIRONMENT_CREDENTIAL_READ_PERMISSION = `${ENVIRONMENT_CREDENTIAL_PERMISSION_PREFIX}r` as const;
export const ENVIRONMENT_CREDENTIAL_CREATE_PERMISSION = `${ENVIRONMENT_CREDENTIAL_PERMISSION_PREFIX}c` as const;
export const ENVIRONMENT_CREDENTIAL_UPDATE_PERMISSION = `${ENVIRONMENT_CREDENTIAL_PERMISSION_PREFIX}u` as const;
export const ENVIRONMENT_CREDENTIAL_DELETE_PERMISSION = `${ENVIRONMENT_CREDENTIAL_PERMISSION_PREFIX}d` as const;

// Typed values are stored as secrets, so storing or rotating them also needs the secret permission.
export const ENVIRONMENT_SECRET_CREATE_PERMISSION = 'environment-secret-c' as const;
export const ENVIRONMENT_SECRET_UPDATE_PERMISSION = 'environment-secret-u' as const;
