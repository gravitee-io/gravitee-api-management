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
import {
    Alert,
    AlertDescription,
    Button,
    Card,
    Checkbox,
    Empty,
    EmptyContent,
    EmptyDescription,
    EmptyHeader,
    EmptyTitle,
    Skeleton,
    cn,
} from '@gravitee/graphene-core';
import { useQueryClient } from '@tanstack/react-query';
import { useCallback, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';

import { notify } from '../../../../../shared/notify';
import { useOrgTags } from '../../../../apis/hooks/useOrgTags';
import { useApiProductDetail } from '../../../hooks/useApiProductDetail';
import { useApiProductResourcePermissions } from '../../../hooks/useApiProductPermissions';
import { updateApiProductShardingTags } from '../../../services/apiProduct';
import { apiProductKeys } from '../../../utils/queryKeys';

export function ApiProductDeploymentConfigurationPage() {
    const { productId } = useParams<{ productId: string }>();
    const env = useEnvironment();
    const queryClient = useQueryClient();

    const { data: product, isLoading: productLoading } = useApiProductDetail(productId);
    const { data: orgTags = [], isLoading: tagsLoading } = useOrgTags();
    const { canUpdate, isLoading: permsLoading } = useApiProductResourcePermissions(productId, 'DEFINITION');
    const isReadOnly = !canUpdate;

    const [editedTags, setEditedTags] = useState<string[] | null>(null);
    const selectedTagIds = editedTags ?? product?.tags ?? [];
    const [isSaving, setIsSaving] = useState(false);

    const isDirty = useMemo(() => {
        if (editedTags === null) return false;
        return [...editedTags].sort().join(',') !== [...(product?.tags ?? [])].sort().join(',');
    }, [editedTags, product]);

    const handleToggle = useCallback(
        (tagId: string) => {
            if (isReadOnly) return;
            setEditedTags(prev => {
                const current = prev ?? product?.tags ?? [];
                return current.includes(tagId) ? current.filter(id => id !== tagId) : [...current, tagId];
            });
        },
        [product, isReadOnly],
    );

    const handleSave = useCallback(async () => {
        if (!productId) return;
        const tagsToSave = editedTags ?? product?.tags ?? [];
        setIsSaving(true);
        try {
            await updateApiProductShardingTags(env!.id, productId, tagsToSave);
            await queryClient.invalidateQueries({ queryKey: apiProductKeys.detail(env!.id, productId) });
            setEditedTags(null);
            notify.success('Deployment configuration saved');
        } catch (e) {
            notify.error(e, 'Failed to save changes.');
        } finally {
            setIsSaving(false);
        }
    }, [productId, env, queryClient, editedTags, product]);

    const handleDiscard = useCallback(() => {
        setEditedTags(null);
    }, []);

    const isLoading = productLoading || tagsLoading || permsLoading;

    return (
        <div className="space-y-4">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Deployment Configuration</h1>
                <p className="text-sm text-muted-foreground">
                    Control where this API Product is deployed on the gateway mesh. Only gateway instances advertising matching sharding
                    tags will load it.
                </p>
            </div>

            <Card className="rounded-xl p-4 sm:p-6 space-y-4">
                <div>
                    <h3 className="text-sm font-semibold">Sharding tags</h3>
                    <p className="text-xs text-muted-foreground mt-0.5">
                        Choose one or more tags. Gateways advertise matching tags; only those instances will load this API Product.
                    </p>
                </div>

                {isReadOnly && !isLoading ? (
                    <Alert>
                        <AlertDescription>You do not have permission to change sharding tags for this API Product.</AlertDescription>
                    </Alert>
                ) : null}

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
                            const checked = selectedTagIds.includes(tag.key);
                            return (
                                <label
                                    key={tag.id}
                                    className={cn(
                                        'flex items-start gap-3 rounded-lg border p-3 transition-colors',
                                        isReadOnly ? 'cursor-not-allowed opacity-60' : 'cursor-pointer',
                                        checked ? 'border-primary/40 bg-primary/5' : 'border-border',
                                        !checked && !isReadOnly && 'hover:border-primary/30 hover:bg-muted/40',
                                    )}
                                >
                                    <Checkbox
                                        checked={checked}
                                        disabled={isReadOnly}
                                        onCheckedChange={() => handleToggle(tag.key)}
                                        className="mt-0.5 shrink-0"
                                    />
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
