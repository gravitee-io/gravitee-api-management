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

import { Categories, Category } from './categories';

export function fakeCategoriesResponse(modifier?: Partial<Categories> | ((baseCategory: Categories) => Categories)): Categories {
  const base: Categories = {
    data: [
      {
        id: 'nkapicategorytest',
        name: 'NKAPICategoryTEST',
        description: 'NKAPICategoryTEST',
        order: 0,
        total_apis: 4,
        _links: {
          picture: 'testpicture',
          background: 'testbackground',
        },
      },
    ],
    metadata: {
      data: {
        total: 1,
      },
    },
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return isFunction(modifier)
    ? modifier(base)
    : {
        ...base,
        ...modifier,
      };
}

export function fakeCategory(modifier?: Partial<Category> | ((baseCategory: Category) => Category)): Category {
  const base: Category = {
    id: 'category-1',
    name: 'Category 1',
    description: 'My first category',
    order: 0,
    total_apis: 4,
    _links: {
      picture: 'test-picture',
      background: 'test-background',
    },
  };

  return isFunction(modifier)
    ? modifier(base)
    : {
        ...base,
        ...modifier,
      };
}
