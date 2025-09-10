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
import { get, has } from 'lodash';

import { Api as ApiV1V2FromMAPIV1 } from '../entities/api';
import { ApiV4, ApiV2 as ApiV2FromMAPIV2, ApiFederated } from '../entities/management-api-v2';

export const isApiV1V2FromMAPIV1 = (api: ApiV1V2FromMAPIV1 | ApiV4 | ApiFederated): api is ApiV1V2FromMAPIV1 => {
  return !isApiV4(api);
};

export const isApiV2FromMAPIV2 = (api: ApiV2FromMAPIV2 | ApiV4 | ApiFederated): api is ApiV2FromMAPIV2 => {
  return !isApiV4(api);
};

export const isApiV4 = (api: unknown): api is ApiV4 => {
  return has(api, 'definitionVersion') && get(api, 'definitionVersion') === 'V4';
};
