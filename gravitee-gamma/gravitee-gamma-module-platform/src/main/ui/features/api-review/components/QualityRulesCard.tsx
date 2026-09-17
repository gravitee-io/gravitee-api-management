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
import { Button, Card, CardContent, CardHeader, CardTitle, Skeleton } from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import { useHasPermission } from '@gravitee/gamma-modules-sdk';

import { QualityRuleDeleteDialog } from './QualityRuleDeleteDialog';
import { QualityRuleSheet } from './QualityRuleSheet';
import { QualityRulesTable } from './QualityRulesTable';
import { SectionError } from '../../../shared/components/SectionError';
import { notify } from '../../../shared/notify';
import { useCreateQualityRule, useDeleteQualityRule, useUpdateQualityRule } from '../hooks/useQualityRuleMutations';
import { useQualityRules } from '../hooks/useQualityRules';
import type { QualityRule, QualityRuleWrite } from '../types/qualityRule';

type SheetState = { type: 'closed' } | { type: 'create' } | { type: 'edit'; rule: QualityRule } | { type: 'delete'; rule: QualityRule };

/** Manual rules reviewers tick when they accept or reject an API (Classic Settings → API Quality → Manual rules). */
export function QualityRulesCard() {
    const canCreate = useHasPermission({ anyOf: ['environment-quality_rule-c'] });
    const canEdit = useHasPermission({ anyOf: ['environment-quality_rule-u'] });
    const canDelete = useHasPermission({ anyOf: ['environment-quality_rule-d'] });

    const { data: rules = [], isLoading, isError } = useQualityRules();
    const createMutation = useCreateQualityRule();
    const updateMutation = useUpdateQualityRule();
    const deleteMutation = useDeleteQualityRule();

    const [sheet, setSheet] = useState<SheetState>({ type: 'closed' });

    function closeSheet() {
        setSheet({ type: 'closed' });
    }

    async function handleSubmit(write: QualityRuleWrite) {
        if (sheet.type === 'edit') {
            try {
                await updateMutation.mutateAsync({ rule: sheet.rule, write });
                notify.success('Manual rule updated successfully');
                closeSheet();
            } catch (error) {
                notify.error(error, 'Failed to update manual rule');
            }
            return;
        }
        try {
            await createMutation.mutateAsync(write);
            notify.success('Manual rule created successfully');
            closeSheet();
        } catch (error) {
            notify.error(error, 'Failed to create manual rule');
        }
    }

    async function handleDelete() {
        if (sheet.type !== 'delete') return;
        try {
            await deleteMutation.mutateAsync(sheet.rule.id);
            notify.success(`“${sheet.rule.name}” has been deleted`);
            closeSheet();
        } catch (error) {
            notify.error(error, 'Failed to delete manual rule');
        }
    }

    function renderContent() {
        if (isLoading) {
            return (
                <div className="space-y-2 px-6 pb-6">
                    {Array.from({ length: 3 }).map((_, i) => (
                        <Skeleton key={i} className="h-12 w-full rounded-md" />
                    ))}
                </div>
            );
        }
        if (isError) {
            return (
                <div className="px-6 pb-6">
                    <SectionError message="Failed to load manual rules. Please refresh and try again." />
                </div>
            );
        }
        return (
            <QualityRulesTable
                rules={rules}
                canEdit={canEdit}
                canDelete={canDelete}
                onEdit={rule => setSheet({ type: 'edit', rule })}
                onDelete={rule => setSheet({ type: 'delete', rule })}
            />
        );
    }

    return (
        <Card>
            <CardHeader className="flex flex-row items-start justify-between gap-3 space-y-0">
                <div className="space-y-1">
                    <CardTitle>API Review Rules</CardTitle>
                    <p className="text-sm text-muted-foreground">Reviewers see these as checkboxes when they accept or reject an API.</p>
                </div>
                {canCreate && (
                    <Button type="button" className="shrink-0" onClick={() => setSheet({ type: 'create' })}>
                        <PlusIcon className="size-4" aria-hidden />
                        Add manual rule
                    </Button>
                )}
            </CardHeader>
            <CardContent className="p-0">{renderContent()}</CardContent>

            <QualityRuleSheet
                open={sheet.type === 'create' || sheet.type === 'edit'}
                rule={sheet.type === 'edit' ? sheet.rule : undefined}
                onClose={closeSheet}
                onSubmit={write => void handleSubmit(write)}
                isSaving={createMutation.isPending || updateMutation.isPending}
            />
            <QualityRuleDeleteDialog
                open={sheet.type === 'delete'}
                rule={sheet.type === 'delete' ? sheet.rule : undefined}
                onClose={closeSheet}
                onConfirm={() => void handleDelete()}
                isDeleting={deleteMutation.isPending}
            />
        </Card>
    );
}
