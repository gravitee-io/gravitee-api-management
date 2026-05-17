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
import { Button, Card, CardContent } from '@gravitee/graphene-core';
import { AlertCircleIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import type { UseMutationResult } from '@tanstack/react-query';
import { useState } from 'react';

import { ApplicationDeleteDialog } from './ApplicationDeleteDialog';
import type { ApplicationListItem } from '../../types/application';

export interface ApplicationLifecycleSectionProps {
    readonly application: ApplicationListItem;
    readonly canDelete: boolean;
    readonly isMutating: boolean;
    readonly deleteMutation: UseMutationResult<ApplicationListItem, Error, void, unknown>;
}

export function ApplicationLifecycleSection({ application, canDelete, isMutating, deleteMutation }: ApplicationLifecycleSectionProps) {
    const [deleteOpen, setDeleteOpen] = useState(false);

    if (!canDelete) {
        return null;
    }

    const deleteError = deleteMutation.isError
        ? deleteMutation.error instanceof Error
            ? deleteMutation.error.message
            : 'Failed to delete application.'
        : null;

    return (
        <>
            <Card className="border-primary">
                <CardContent className="py-4">
                    <div className="flex items-center justify-between gap-4">
                        <div className="flex items-center gap-3">
                            <AlertCircleIcon className="size-4 shrink-0 text-destructive" aria-hidden />
                            <div>
                                <p className="text-sm font-medium text-destructive">Delete this Application</p>
                                <p className="text-xs text-muted-foreground">
                                    Archiving closes active subscriptions and moves this application to the archived list.
                                </p>
                            </div>
                        </div>
                        <Button
                            type="button"
                            size="sm"
                            className="shrink-0 bg-primary text-primary-foreground hover:bg-primary/90"
                            disabled={isMutating}
                            onClick={() => setDeleteOpen(true)}
                        >
                            <Trash2Icon className="size-3.5 text-primary-foreground" aria-hidden />
                            Delete
                        </Button>
                    </div>
                </CardContent>
            </Card>

            <ApplicationDeleteDialog
                open={deleteOpen}
                onOpenChange={setDeleteOpen}
                applicationName={application.name}
                onDelete={() => deleteMutation.mutate()}
                isLoading={deleteMutation.isPending}
                error={deleteError}
            />
        </>
    );
}
