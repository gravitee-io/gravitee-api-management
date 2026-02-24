/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { getApiAccess } from './api-access.util';

import { Api, ApiV2, ApiV4 } from '../../entities/management-api-v2';

export function getApiContextPath(api: Api | null | undefined): string | null {
  if (!api || (api.definitionVersion !== 'V2' && api.definitionVersion !== 'V4')) return null;
  const access = getApiAccess(api as ApiV4 | ApiV2);
  return access?.[0] ?? null;
}

export const mapDefinitionVersionToLabel = (definitionVersion: string): string => {
  switch (definitionVersion) {
    case 'V1':
      return '1.0.0';
    case 'V2':
      return '2.0.0';
    case 'V4':
      return '4.0.0';
    default:
      return definitionVersion;
  }
};
