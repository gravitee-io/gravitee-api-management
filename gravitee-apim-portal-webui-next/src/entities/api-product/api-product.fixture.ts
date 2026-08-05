/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { isFunction } from 'lodash';

import { ApiProduct, ApiProductApi } from './api-product';

export function fakeApiProductApi(modifier?: Partial<ApiProductApi> | ((baseApi: ApiProductApi) => ApiProductApi)): ApiProductApi {
  const base: ApiProductApi = {
    id: 'api-1',
    name: 'API 1',
    version: '1.0.0',
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeApiProduct(modifier?: Partial<ApiProduct> | ((baseApiProduct: ApiProduct) => ApiProduct)): ApiProduct {
  const base: ApiProduct = {
    id: '4f6597ca-74b8-4e68-a597-ca74b83e6824',
    name: 'API Product 1',
    description: 'API Product description',
    version: '1.0.0',
    kind: 'AI_WORKSPACE',
    navigationItemId: '9d1dfa42-c550-4ca3-9dfa-42c550dca37a',
    tags: ['AI', 'Workspace'],
    apis: [fakeApiProductApi()],
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}
