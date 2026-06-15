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
import { Badge, Skeleton } from '@gravitee/graphene-core';

import { useComponentModels } from '../../hooks/useAiProductHooks';

/**
 * Models served by an LLM proxy component, read from its endpoint connector
 * configuration. Rendered in the expanded component row.
 */
export function ComponentModelsList({ apiId }: { apiId: string }) {
    const { data: models, isLoading, isError } = useComponentModels(apiId);

    if (isLoading) {
        return (
            <div className="space-y-1.5">
                <Skeleton className="h-5 w-48 rounded" />
                <Skeleton className="h-5 w-36 rounded" />
            </div>
        );
    }
    if (isError) {
        return <p className="text-xs text-destructive">Failed to load models for this LLM proxy.</p>;
    }
    if (!models || models.length === 0) {
        return <p className="text-xs text-muted-foreground">No models configured on this LLM proxy.</p>;
    }

    return (
        <div className="space-y-1.5">
            <p className="text-xs font-medium text-muted-foreground">Available models</p>
            <div className="flex flex-wrap gap-1.5">
                {models.map(model => (
                    <Badge key={model.name} variant="outline" className="text-xs font-mono gap-1.5">
                        {model.name}
                        {model.aliases && model.aliases.length > 0 ? (
                            <span className="text-muted-foreground font-sans">({model.aliases.join(', ')})</span>
                        ) : null}
                    </Badge>
                ))}
            </div>
        </div>
    );
}
