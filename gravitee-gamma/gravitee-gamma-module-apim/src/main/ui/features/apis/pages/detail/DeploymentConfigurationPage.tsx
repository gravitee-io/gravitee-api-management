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
import { Button, Card, Checkbox, Empty, EmptyContent, EmptyHeader, EmptyTitle, EmptyDescription, Skeleton } from '@gravitee/graphene-core';
import { useQueryClient } from '@tanstack/react-query';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';

import { useApimRuntime } from '../../../../core/context/apimRuntimeContext';
import { updateApiShardingTags } from '../../../../services/apis/apis';
import { apiDetailKeys } from '../../../../utils/queryKeys';
import { useApiDetail } from '../../hooks/useApiDetail';
import { useOrgTags } from '../../hooks/useOrgTags';

export function DeploymentConfigurationPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const runtime = useApimRuntime();
    const queryClient = useQueryClient();

    const { data: api, isLoading: apiLoading } = useApiDetail(apiId);
    const { data: orgTags = [], isLoading: tagsLoading } = useOrgTags();

    const initialTagIds = useMemo(() => api?.tags ?? [], [api]);
    const [selectedTagIds, setSelectedTagIds] = useState<string[]>([]);
    const [isSaving, setIsSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        setSelectedTagIds(api?.tags ?? []);
    }, [api]);

    const isDirty = useMemo(() => {
        const a = [...selectedTagIds].sort().join(',');
        const b = [...initialTagIds].sort().join(',');
        return a !== b;
    }, [selectedTagIds, initialTagIds]);

    const handleToggle = useCallback((tagId: string) => {
        setSelectedTagIds(prev => (prev.includes(tagId) ? prev.filter(id => id !== tagId) : [...prev, tagId]));
    }, []);

    const handleSave = useCallback(async () => {
        if (!apiId) return;
        setIsSaving(true);
        setError(null);
        try {
            await updateApiShardingTags(runtime, apiId, selectedTagIds);
            await queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(runtime, apiId) });
        } catch (e) {
            setError(e instanceof Error ? e.message : 'Failed to save changes.');
        } finally {
            setIsSaving(false);
        }
    }, [apiId, runtime, queryClient, selectedTagIds]);

    const handleDiscard = useCallback(() => {
        setSelectedTagIds(api?.tags ?? []);
        setError(null);
    }, [api]);

    const isLoading = apiLoading || tagsLoading;

    return (
        <div className="space-y-4">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Deployment Configuration</h1>
                <p className="text-sm text-muted-foreground">
                    Control where this API is deployed on the gateway mesh (sharding tags). Only gateway instances advertising matching tags
                    will load this API definition.
                </p>
            </div>

            {error ? (
                <Card className="rounded-xl border border-destructive/30 bg-destructive/5 p-4">
                    <p className="text-sm text-destructive">{error}</p>
                </Card>
            ) : null}

            <Card className="rounded-xl p-4 sm:p-6 space-y-4">
                <div>
                    <h3 className="text-sm font-semibold">Sharding tags</h3>
                    <p className="text-xs text-muted-foreground mt-0.5">
                        Choose one or more tags. Gateways advertise matching tags; only those instances will load this API definition.
                    </p>
                </div>

                {isLoading ? (
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                        {Array.from({ length: 4 }).map((_, i) => (
                            <Skeleton key={i} className="h-14 rounded-lg" />
                        ))}
                    </div>
                ) : orgTags.length === 0 ? (
                    <Empty>
                        <EmptyHeader>
                            <EmptyTitle>No sharding tags configured</EmptyTitle>
                            <EmptyDescription>Sharding tags are managed at the organisation level under Gateway settings.</EmptyDescription>
                        </EmptyHeader>
                        <EmptyContent />
                    </Empty>
                ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                        {orgTags.map(tag => {
                            const checked = selectedTagIds.includes(tag.id);
                            return (
                                <label
                                    key={tag.id}
                                    className={`flex items-start gap-3 rounded-lg border p-3 cursor-pointer transition-colors ${
                                        checked
                                            ? 'border-primary/40 bg-primary/5'
                                            : 'border-border hover:border-primary/25 hover:bg-muted/40'
                                    }`}
                                >
                                    <Checkbox checked={checked} onCheckedChange={() => handleToggle(tag.id)} className="mt-0.5 shrink-0" />
                                    <div className="min-w-0">
                                        <p className="text-sm font-medium leading-snug">{tag.name}</p>
                                        {tag.description ? <p className="text-xs text-muted-foreground mt-0.5">{tag.description}</p> : null}
                                    </div>
                                </label>
                            );
                        })}
                    </div>
                )}
            </Card>

            {isDirty ? (
                <div className="flex items-center justify-end gap-3 border-t pt-4">
                    <Button type="button" variant="outline" size="sm" onClick={handleDiscard} disabled={isSaving}>
                        Discard
                    </Button>
                    <Button type="button" size="sm" onClick={handleSave} disabled={isSaving}>
                        {isSaving ? 'Saving…' : 'Save changes'}
                    </Button>
                </div>
            ) : null}
        </div>
    );
}
