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

import { Alert, AlertDescription, Button, Skeleton } from '@gravitee/graphene-core';
import { CheckIcon, InfoIcon } from '@gravitee/graphene-core/icons';
import type { ReactNode } from 'react';

export function OrgSettingsFormShell({
    title,
    description,
    canEdit,
    isDirty,
    isValid = true,
    isSaving,
    isLoading,
    isError,
    onSave,
    onDiscard,
    children,
}: Readonly<{
    title: string;
    description: string;
    canEdit: boolean;
    isDirty: boolean;
    isValid?: boolean;
    isSaving: boolean;
    isLoading: boolean;
    isError: boolean;
    onSave: () => void;
    onDiscard: () => void;
    children: ReactNode;
}>) {
    if (isLoading) {
        return (
            <div className="space-y-6">
                <Skeleton className="h-10 w-64" />
                <Skeleton className="h-96 w-full rounded-lg" />
            </div>
        );
    }

    if (isError) {
        return (
            <div className="space-y-6">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
                    <p className="text-sm text-muted-foreground">{description}</p>
                </div>
                <div className="flex items-center justify-center rounded-lg border p-8">
                    <p className="text-sm text-muted-foreground">Failed to load settings. Please refresh and try again.</p>
                </div>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
                    <p className="text-sm text-muted-foreground">{description}</p>
                </div>
                {isDirty && canEdit ? (
                    <div className="flex shrink-0 items-center gap-2">
                        <Button variant="outline" size="sm" onClick={onDiscard} disabled={isSaving}>
                            Discard
                        </Button>
                        <Button size="sm" onClick={onSave} disabled={isSaving || !isValid}>
                            <CheckIcon className="size-4" aria-hidden />
                            {isSaving ? 'Saving…' : 'Save changes'}
                        </Button>
                    </div>
                ) : null}
            </div>

            {!canEdit ? (
                <Alert>
                    <InfoIcon className="size-4" />
                    <AlertDescription>
                        You do not have permission to modify these settings. Contact your administrator for access.
                    </AlertDescription>
                </Alert>
            ) : null}

            {children}
        </div>
    );
}
