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
import { Button, Card, CardContent, CardDescription, CardHeader, CardTitle, Skeleton } from '@gravitee/graphene-core';
import { UploadIcon } from '@gravitee/graphene-core/icons';

import { RulesetItem } from './RulesetItem';
import type { ScoringRuleset } from '../../types/apiScore';

interface RulesetListProps {
    rulesets: ScoringRuleset[];
    isLoading: boolean;
    isError: boolean;
    canImport: boolean;
    canEdit: boolean;
    canDelete: boolean;
    onImport: () => void;
    onEdit: (ruleset: ScoringRuleset) => void;
    onDelete: (ruleset: ScoringRuleset) => void;
}

export function RulesetList({ rulesets, isLoading, isError, canImport, canEdit, canDelete, onImport, onEdit, onDelete }: RulesetListProps) {
    return (
        <Card>
            <CardHeader>
                <div className="flex items-start justify-between gap-4">
                    <div className="space-y-1">
                        <CardTitle>Rulesets</CardTitle>
                        <CardDescription>
                            Custom rulesets allow you to enforce your organization&apos;s API design, quality and security standards.
                        </CardDescription>
                    </div>
                    {canImport ? (
                        <Button type="button" variant="outline" size="sm" className="shrink-0" onClick={onImport}>
                            <UploadIcon className="size-4" aria-hidden />
                            Import
                        </Button>
                    ) : null}
                </div>
            </CardHeader>
            <CardContent className="space-y-2">
                {isLoading ? (
                    <>
                        <Skeleton className="h-12 w-full rounded-lg" />
                        <Skeleton className="h-12 w-full rounded-lg" />
                    </>
                ) : isError ? (
                    <p className="rounded-lg border border-destructive/30 bg-destructive/5 py-8 text-center text-sm text-destructive">
                        Failed to load rulesets. Please refresh and try again.
                    </p>
                ) : rulesets.length === 0 ? (
                    <p className="rounded-lg border border-dashed py-8 text-center text-sm text-muted-foreground">No ruleset, yet.</p>
                ) : (
                    rulesets.map(ruleset => (
                        <RulesetItem
                            key={ruleset.id}
                            ruleset={ruleset}
                            canEdit={canEdit}
                            canDelete={canDelete}
                            onEdit={onEdit}
                            onDelete={onDelete}
                        />
                    ))
                )}
            </CardContent>
        </Card>
    );
}
