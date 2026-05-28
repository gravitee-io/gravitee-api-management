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
import { Card, CardContent, Skeleton } from '@gravitee/graphene-core';

export function ContentSkeleton() {
    return (
        <div className="space-y-6" aria-busy="true" aria-label="Loading content">
            <div className="space-y-1">
                <Skeleton className="h-8 w-64 rounded" />
                <Skeleton className="h-4 w-96 rounded" />
            </div>

            <Card>
                <CardContent className="pt-5 pb-5 space-y-4">
                    <div className="flex items-start gap-3">
                        <Skeleton className="size-9 rounded-lg shrink-0" />
                        <div className="space-y-1.5 flex-1">
                            <Skeleton className="h-4 w-40 rounded" />
                            <Skeleton className="h-3 w-72 rounded" />
                        </div>
                    </div>
                    <div className="grid grid-cols-1 lg:grid-cols-3 gap-3">
                        {Array.from({ length: 3 }, (_, i) => (
                            <Skeleton key={i} className="h-20 rounded" />
                        ))}
                    </div>
                </CardContent>
            </Card>

            <div className="space-y-3">
                <div className="space-y-0.5">
                    <Skeleton className="h-6 w-36 rounded" />
                    <Skeleton className="h-4 w-56 rounded" />
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    {Array.from({ length: 6 }, (_, i) => (
                        <Card key={i}>
                            <CardContent className="pt-5 pb-5">
                                <div className="space-y-3">
                                    <div className="flex items-center gap-3">
                                        <Skeleton className="size-10 rounded-lg shrink-0" />
                                        <div className="space-y-1.5 flex-1">
                                            <Skeleton className="h-4 w-28 rounded" />
                                            <Skeleton className="h-3 w-48 rounded" />
                                        </div>
                                    </div>
                                    <Skeleton className="h-3 w-full rounded" />
                                    <Skeleton className="h-3 w-3/4 rounded" />
                                </div>
                            </CardContent>
                        </Card>
                    ))}
                </div>
            </div>
        </div>
    );
}
