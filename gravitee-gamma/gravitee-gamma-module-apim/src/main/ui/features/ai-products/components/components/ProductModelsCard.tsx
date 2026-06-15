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
import { Badge, Card, CardContent, Skeleton } from '@gravitee/graphene-core';
import { useMemo } from 'react';

import { useProductModels } from '../../hooks/useAiProductHooks';
import { providerLabel } from '../../services/aiProduct';

/**
 * Models the product exposes, grouped by upstream provider — the "one product, many providers"
 * (OpenAI + Anthropic + Google …) story consumers see through a single key.
 */
export function ProductModelsCard({ apiIds }: { apiIds: string[] }) {
    const { data: models, isLoading } = useProductModels(apiIds);

    const byProvider = useMemo(() => {
        const map = new Map<string, string[]>();
        for (const model of models ?? []) {
            const label = providerLabel(model.provider);
            const list = map.get(label) ?? [];
            list.push(model.name);
            map.set(label, list);
        }
        return [...map.entries()];
    }, [models]);

    if (apiIds.length === 0) return null;

    return (
        <Card>
            <CardContent className="pt-5 pb-4 space-y-3">
                <div className="flex items-baseline justify-between">
                    <p className="text-sm font-medium">Available models</p>
                    <p className="text-xs text-muted-foreground">
                        {isLoading
                            ? ''
                            : `${models?.length ?? 0} models · ${byProvider.length} provider${byProvider.length === 1 ? '' : 's'}`}
                    </p>
                </div>

                {isLoading ? (
                    <div className="space-y-2">
                        <Skeleton className="h-5 w-64 rounded" />
                        <Skeleton className="h-5 w-48 rounded" />
                    </div>
                ) : byProvider.length === 0 ? (
                    <p className="text-xs text-muted-foreground">No models configured on the attached LLM proxies yet.</p>
                ) : (
                    <div className="space-y-2.5">
                        {byProvider.map(([provider, modelNames]) => (
                            <div key={provider} className="flex items-start gap-3">
                                <Badge variant="secondary" className="text-xs shrink-0 mt-0.5">
                                    {provider}
                                </Badge>
                                <div className="flex flex-wrap gap-1.5">
                                    {modelNames.map(name => (
                                        <Badge key={name} variant="outline" className="text-xs font-mono">
                                            {name}
                                        </Badge>
                                    ))}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </CardContent>
        </Card>
    );
}
