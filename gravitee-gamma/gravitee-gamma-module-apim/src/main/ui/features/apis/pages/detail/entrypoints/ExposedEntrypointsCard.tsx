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
import { Card, CardContent, Skeleton } from '@gravitee/graphene-core';

import { CopyButton } from './CopyButton';
import { InfoTooltip } from './InfoTooltip';

function entrypointLabel(index: number, virtualHostMode: boolean): string {
    if (virtualHostMode) return `Virtual host row ${index + 1}`;
    if (index === 0) return 'Gateway URL';
    if (index === 1) return 'Internal URL';
    return `URL ${index + 1}`;
}

interface ExposedEntrypointsCardProps {
    entrypoints: { value: string }[];
    isLoading: boolean;
    virtualHostMode: boolean;
}

export function ExposedEntrypointsCard({ entrypoints, isLoading, virtualHostMode }: Readonly<ExposedEntrypointsCardProps>) {
    return (
        <Card>
            <CardContent className="p-5 space-y-4">
                <div>
                    <div className="text-sm font-semibold text-foreground flex items-center">
                        Exposed entrypoints
                        <InfoTooltip text="URLs derived from your context paths or virtual hosts—the same values consumers see in the Developer Portal." />
                    </div>
                    <p className="mt-0.5 text-xs text-muted-foreground">
                        Preview of gateway URLs for the paths configured above (plus internal routing where applicable).
                    </p>
                </div>

                {isLoading ? (
                    <div className="space-y-2">
                        <Skeleton className="h-12 w-full rounded-lg" />
                        <Skeleton className="h-12 w-full rounded-lg" />
                    </div>
                ) : entrypoints.length === 0 ? (
                    <p className="text-xs text-muted-foreground italic">No exposed entrypoints available.</p>
                ) : (
                    <div className="space-y-2">
                        {entrypoints.map((ep, index) => (
                            <div
                                key={ep.value}
                                className="flex items-center justify-between rounded-lg border px-3 py-2.5 gap-3"
                                style={{ backgroundColor: 'color-mix(in oklab, var(--color-muted) 40%, transparent)' }}
                            >
                                <div className="min-w-0">
                                    <p className="text-xs text-muted-foreground">{entrypointLabel(index, virtualHostMode)}</p>
                                    <p className="text-sm font-medium font-mono truncate">{ep.value}</p>
                                </div>
                                <CopyButton value={ep.value} />
                            </div>
                        ))}
                    </div>
                )}
            </CardContent>
        </Card>
    );
}
