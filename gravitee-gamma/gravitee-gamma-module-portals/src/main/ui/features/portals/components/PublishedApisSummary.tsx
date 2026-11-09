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

import { Card, CardContent, CardHeader, Skeleton } from '@gravitee/graphene-core';

interface PublishedApisSummaryProps {
    readonly count?: number;
}

export function PublishedApisSummary({ count }: PublishedApisSummaryProps) {
    return (
        <section aria-label="Published APIs">
            <Card className="h-full gap-3">
                <CardHeader>
                    <h2 className="text-sm font-normal text-muted-foreground">Published APIs</h2>
                </CardHeader>
                <CardContent>
                    {count !== undefined ? (
                        <p className="text-2xl font-semibold">{count}</p>
                    ) : (
                        <Skeleton className="h-9 w-24" aria-label="Loading published APIs" />
                    )}
                </CardContent>
            </Card>
        </section>
    );
}
