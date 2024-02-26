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

import { isFunction } from 'lodash';

import { Metadata } from './metadata';
import { MetadataResponse } from './metadataResponse';

export function fakeMetadata(modifier?: Partial<Metadata> | ((base: Metadata) => Metadata)): Metadata {
  const base: Metadata = {
    key: 'my-key',
    value: 'my-value',
    name: 'my metadata',
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeMetadataResponse(
  modifier?: Partial<MetadataResponse> | ((base: MetadataResponse) => MetadataResponse),
): MetadataResponse {
  const base: MetadataResponse = {
    data: [fakeMetadata()],
    pagination: {
      totalCount: 1,
      page: 1,
      pageCount: 1,
      pageItemsCount: 1,
      perPage: 10,
    },
    links: { self: 'self' },
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}
