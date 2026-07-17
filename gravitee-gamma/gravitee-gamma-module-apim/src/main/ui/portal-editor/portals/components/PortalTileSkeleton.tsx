/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { Skeleton } from '@gravitee/graphene-core';

export function PortalTileSkeleton() {
    return (
        <div className="flex size-full flex-col bg-muted/40 p-3" aria-hidden="true">
            <Skeleton className="mb-3 h-4 w-full rounded-sm" />
            <div className="flex min-h-0 flex-1 gap-2">
                <Skeleton className="hidden w-1/4 rounded-sm sm:block" />
                <div className="flex flex-1 flex-col gap-2">
                    <Skeleton className="h-3 w-3/4 rounded-sm" />
                    <Skeleton className="h-3 w-full rounded-sm" />
                    <Skeleton className="h-3 w-5/6 rounded-sm" />
                    <Skeleton className="mt-auto h-3 w-2/3 rounded-sm" />
                </div>
            </div>
        </div>
    );
}
