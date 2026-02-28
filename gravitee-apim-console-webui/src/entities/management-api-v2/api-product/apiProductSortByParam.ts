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

/** Allowed sortBy values for API Product _search (matches backend enum). */
export type ApiProductSortByParam = 'name' | '-name' | 'version' | '-version' | 'apis' | '-apis' | 'owner' | '-owner';

const ALLOWED_SORT_BY: ApiProductSortByParam[] = ['name', '-name', 'version', '-version', 'apis', '-apis', 'owner', '-owner'];

/**
 * Returns the given string as ApiProductSortByParam if it is allowed, otherwise undefined.
 * Use this to avoid sending invalid sort values to the backend.
 */
export function toApiProductSortByParam(sortBy: string | undefined): ApiProductSortByParam | undefined {
  if (!sortBy) {
    return undefined;
  }
  return ALLOWED_SORT_BY.includes(sortBy as ApiProductSortByParam) ? (sortBy as ApiProductSortByParam) : undefined;
}
