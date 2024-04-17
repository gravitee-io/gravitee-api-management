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
import { isFunction } from 'rxjs/internal/util/isFunction';

import { Page } from './page';
import { PagesResponse } from './pages-response';

export function fakePage(modifier?: Partial<Page> | ((baseApi: Page) => Page)): Page {
  const base: Page = {
    id: 'aee23b1e-34b1-4551-a23b-1e34b165516a',
    name: '\uD83E\uDE90 Planets',
    order: 0,
    type: 'MARKDOWN',
    content: 'markdown content',
    updated_at: new Date(1642675655553),
    _links: {},
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakePagesResponse(modifier?: Partial<PagesResponse> | ((baseApi: PagesResponse) => PagesResponse)): PagesResponse {
  const base: PagesResponse = {
    data: [fakePage()],
    metadata: {},
    links: {},
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}
