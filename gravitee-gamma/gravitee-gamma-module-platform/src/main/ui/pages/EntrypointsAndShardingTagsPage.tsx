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

import { Alert, AlertDescription, Card, CardContent, CardDescription, CardHeader, CardTitle, Skeleton } from '@gravitee/graphene-core';
import { InfoIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import { EntrypointConfigurationSection } from '../features/entrypoints/components/EntrypointConfigurationSection';
import { EntrypointDetailSheet } from '../features/entrypoints/components/EntrypointDetailSheet';
import { EntrypointMappingsTable } from '../features/entrypoints/components/EntrypointMappingsTable';
import { useEntrypointConfigurations } from '../features/entrypoints/hooks/useEntrypointConfigurations';
import { useEntrypointMappings } from '../features/entrypoints/hooks/useEntrypointMappings';
import type { EntrypointMappingRow } from '../features/entrypoints/types/entrypoint';

export function EntrypointsAndShardingTagsPage() {
    const { data: configurationData, isLoading: isConfigurationLoading, isError: isConfigurationError } = useEntrypointConfigurations();
    const { rows, isLoading: isMappingsLoading, isError: isMappingsError, isNameResolutionError } = useEntrypointMappings();

    const [selected, setSelected] = useState<EntrypointMappingRow | null>(null);

    return (
        <div className="space-y-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Entrypoints & Sharding Tags</h1>
                <p className="text-sm text-muted-foreground">
                    View entrypoint configuration and mappings used by the Developer Portal based on API sharding tags.
                </p>
            </div>

            <Alert>
                <InfoIcon className="size-4" aria-hidden />
                <AlertDescription>
                    Include entrypoint and sharding tag configuration according to the values already used by the deployed API Gateway(s).
                </AlertDescription>
            </Alert>

            <EntrypointConfigurationSection
                configs={configurationData?.configs ?? []}
                failedEnvironmentNames={configurationData?.failedEnvironmentNames ?? []}
                isLoading={isConfigurationLoading}
                isError={isConfigurationError}
            />

            <Card>
                <CardHeader className="flex flex-row items-start justify-between gap-4 space-y-0">
                    <div className="space-y-1.5">
                        <CardTitle>Entrypoint Mappings</CardTitle>
                        <CardDescription>Entrypoint to be displayed in the Developer Portal if an API has a given tag</CardDescription>
                    </div>
                </CardHeader>
                <CardContent className="space-y-3">
                    {isNameResolutionError && !isMappingsLoading && !isMappingsError ? (
                        <Alert>
                            <InfoIcon className="size-4" aria-hidden />
                            <AlertDescription>
                                Some environment or sharding tag names could not be loaded. IDs may be shown instead of display names.
                            </AlertDescription>
                        </Alert>
                    ) : null}
                    {isMappingsLoading ? (
                        <div className="space-y-2">
                            {Array.from({ length: 4 }).map((_, i) => (
                                <Skeleton key={i} className="h-12 w-full rounded-md" />
                            ))}
                        </div>
                    ) : isMappingsError ? (
                        <Alert variant="destructive">
                            <AlertDescription>Failed to load entrypoint mappings. Please refresh and try again.</AlertDescription>
                        </Alert>
                    ) : (
                        <EntrypointMappingsTable rows={rows} canCreate={false} onOpenDetail={setSelected} />
                    )}
                </CardContent>
            </Card>

            <EntrypointDetailSheet entrypoint={selected} onClose={() => setSelected(null)} />
        </div>
    );
}
