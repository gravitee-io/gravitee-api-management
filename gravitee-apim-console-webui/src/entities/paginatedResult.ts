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

interface Pagination {
  page: number;
  pageCount: number;
  pageItemCount: number;
  perPage: number;
  totalCount: number;
}

export class PaginatedResult<T = any> {
  public data: T[] = [];
  public pagination: Pagination;

  constructor(data: T[], pagination: Pagination) {
    this.data = data;
    this.pagination = pagination;
  }
}

export const fakePaginatedResult = <T extends Array<any>>(data: T, page?: Pagination): PaginatedResult<T[number]> => {
  return new PaginatedResult<T[number]>(
    data,
    page ?? {
      page: 1,
      perPage: 10,
      pageItemCount: 5,
      totalCount: 5,
      pageCount: 1,
    },
  );
};
