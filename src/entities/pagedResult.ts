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
  readonly current: number;
  readonly size: number;
  readonly per_page: number;
  readonly total_pages: number;
  readonly total_elements: number;
}

export class PagedResult<T = any> {
  public data: T[] = [];
  public metadata: Record<string, Record<string, any>> = {};
  public page: Page = new Page();

  constructor() {
    'ngInject';
  }

  populate(responseData: any) {
    this.data = responseData.data;
    this.metadata = responseData.metadata;
    this.page = responseData.page;
  }
}

export const fakePagedResult = <T extends Array<any>>(
  data: T,
  page?: Page,
  metadata?: Record<string, Record<string, any>>,
): PagedResult<T> => {
  const pr = new PagedResult<T[number]>();
  pr.data = data;
  pr.page = page ?? { current: 1, per_page: 10, size: 5, total_elements: 5, total_pages: 1 };
  pr.metadata = metadata;
  return pr;
};
