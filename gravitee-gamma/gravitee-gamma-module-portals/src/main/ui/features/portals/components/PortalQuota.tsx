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

import type { PortalQuota as Quota } from '../types/portal';

interface PortalQuotaProps {
    readonly quota?: Quota;
}

export function PortalQuota({ quota }: PortalQuotaProps) {
    return (
        <section aria-label="Portal quota">
            <Card className="h-full gap-3">
                <CardHeader>
                    <h2 className="text-sm font-normal text-muted-foreground">Portal quota</h2>
                </CardHeader>
                <CardContent>
                    {quota ? (
                        <p className="text-2xl font-semibold">
                            {quota.used} / {quota.limit}
                        </p>
                    ) : (
                        <Skeleton className="h-9 w-24" aria-label="Loading portal quota" />
                    )}
                </CardContent>
            </Card>
        </section>
    );
}
