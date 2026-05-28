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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import { keepPreviousData, useQuery } from '@tanstack/react-query';

import { listApplications } from '../services/applicationList';
import type { ApplicationListResponse, ApplicationStatus } from '../types/application';
import { applicationListKeys } from '../utils/queryKeys';

export function useApplicationList({
    query,
    status,
    page,
    perPage,
    order,
}: {
    query: string;
    status: ApplicationStatus;
    page: number;
    perPage: number;
    order: string;
}) {
    const env = useEnvironment();
    return useQuery<ApplicationListResponse>({
        queryKey: applicationListKeys.search(env?.id ?? '', query, status, page, perPage, order),
        queryFn: () =>
            listApplications(env!.id, {
                query: query || undefined,
                page,
                size: perPage,
                status,
                order,
            }),
        enabled: Boolean(env),
        staleTime: 30_000,
        placeholderData: keepPreviousData,
    });
}
