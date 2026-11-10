/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import type { PortalsResponse } from '../types/portal';

interface ListPagination {
    readonly page: number;
    readonly pageCount: number | undefined;
    readonly hasPrevious: boolean;
    readonly hasNext: boolean;
}

function isCount(value: number | undefined): value is number {
    return value !== undefined && Number.isSafeInteger(value) && value >= 0;
}

export function getListPagination(response: PortalsResponse, requestedPage: number, requestedPerPage: number): ListPagination {
    const metadata = response.pagination;
    const page = isCount(metadata.page) && metadata.page > 0 ? metadata.page : requestedPage;
    const perPage = isCount(metadata.perPage) && metadata.perPage > 0 ? metadata.perPage : requestedPerPage;
    let pageCount: number | undefined;
    if (isCount(metadata.pageCount)) {
        pageCount = metadata.pageCount;
    } else if (isCount(metadata.totalCount)) {
        pageCount = Math.ceil(metadata.totalCount / perPage);
    }

    return {
        page,
        pageCount,
        hasPrevious: page > 1,
        hasNext: pageCount === undefined ? Boolean(response.links?.next) : page < pageCount,
    };
}
