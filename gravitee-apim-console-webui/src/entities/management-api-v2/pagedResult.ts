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

class Page {
  readonly page: number;
  readonly perPage: number;
  readonly pageCount: number;
  readonly pageItemsCount: number;
  readonly totalCount: number;
}

export class PagedResult<T = any> {
  public data: T[] = [];
  public links: Record<string, string> = {};
  public pagination: Page = new Page();

  populate(responseData: any) {
    this.data = responseData.data;
    this.links = responseData.links;
    this.pagination = responseData.pagination;
  }
}

export const fakePagedResult = <T extends Array<any>>(
  data: T,
  pagination?: Page,
  links?: Record<string, string>,
): PagedResult<T[number]> => {
  const pr = new PagedResult<T[number]>();
  pr.data = data;
  pr.pagination = pagination ?? {
    page: 1,
    perPage: 10,
    pageCount: 1,
    pageItemsCount: 10,
    totalCount: 10,
  };
  pr.links = links;
  return pr;
};
