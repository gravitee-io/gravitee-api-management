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

import { Alert, AlertDescription, Badge, Button, Skeleton } from '@gravitee/graphene-core';
import { ArrowLeftIcon, CheckIcon, InfoIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import { NotificationTemplateChannelCard } from '../features/notification-templates/components/NotificationTemplateChannelCard';
import { useNotificationTemplate, useSaveNotificationTemplates } from '../features/notification-templates/hooks/useNotificationTemplate';
import { useNotificationTemplatePermissions } from '../features/notification-templates/hooks/useNotificationTemplatePermissions';
import type {
    NotificationTemplate,
    NotificationTemplateDraft,
    NotificationTemplateType,
} from '../features/notification-templates/types/notificationTemplate';
import {
    CHANNEL_ORDER,
    canPersistChannel,
    channelDraftsEqual,
    isTemplatesToInclude,
    scopeLabel,
    toPersistedTemplate,
    validateChannelDraft,
} from '../features/notification-templates/utils/templateDisplay';
import { notify } from '../shared/notify';

function toDraft(template: NotificationTemplate): NotificationTemplateDraft {
    return {
        enabled: template.enabled === true,
        title: template.title ?? '',
        content: template.content,
    };
}

function draftsFromTemplates(templates: readonly NotificationTemplate[]): Record<string, NotificationTemplateDraft> {
    return Object.fromEntries(templates.map(template => [template.type, toDraft(template)]));
}

export function NotificationTemplateDetailPage() {
    const { scope = '', hook: hookParam = '' } = useParams<{ scope: string; hook: string }>();
    const hookOrName = decodeURIComponent(hookParam);
    const { canCreate, canUpdate, canEdit } = useNotificationTemplatePermissions();
    const { data: templates = [], isLoading, isError, refetch } = useNotificationTemplate(scope, hookOrName);
    const saveMutation = useSaveNotificationTemplates();

    const [drafts, setDrafts] = useState<Record<string, NotificationTemplateDraft>>({});
    const [savedDrafts, setSavedDrafts] = useState<Record<string, NotificationTemplateDraft>>({});
    const [showErrors, setShowErrors] = useState(false);

    const isDirty = !channelDraftsEqual(drafts, savedDrafts);
    const isDirtyRef = useRef(isDirty);
    isDirtyRef.current = isDirty;

    useEffect(() => {
        if (templates.length === 0) {
            return;
        }
        const next = draftsFromTemplates(templates);
        if (!isDirtyRef.current) {
            setSavedDrafts(next);
            setDrafts(next);
            setShowErrors(false);
        }
    }, [templates]);

    const isInclude = isTemplatesToInclude(scope);
    const first = templates[0];
    const orderedTemplates = useMemo(
        () =>
            CHANNEL_ORDER.map(type => templates.find(template => template.type === type)).filter((item): item is NotificationTemplate =>
                Boolean(item),
            ),
        [templates],
    );

    function draftFor(template: NotificationTemplate): NotificationTemplateDraft {
        return drafts[template.type] ?? toDraft(template);
    }

    const invalid = orderedTemplates.some(template => validateChannelDraft(draftFor(template), isInclude).length > 0);

    function handleDraftChange(type: NotificationTemplateType, next: NotificationTemplateDraft) {
        setDrafts(current => ({ ...draftsFromTemplates(templates), ...current, [type]: next }));
    }

    async function handleSave() {
        if (!canEdit || saveMutation.isPending) {
            return;
        }
        if (invalid) {
            setShowErrors(true);
            return;
        }
        const persistable = orderedTemplates.filter(template => canPersistChannel(template, canCreate, canUpdate));
        if (persistable.length === 0) {
            return;
        }
        const payload = persistable.map(template => toPersistedTemplate(template, draftFor(template)));
        try {
            await saveMutation.mutateAsync(payload);
            notify.success('Template has been successfully saved!');
            setSavedDrafts(drafts);
            setShowErrors(false);
        } catch (error) {
            notify.error(error, 'Failed to save notification template.');
        }
    }

    if (isLoading) {
        return (
            <div className="space-y-4">
                <Skeleton className="h-8 w-48" />
                <Skeleton className="h-40 w-full rounded-xl" />
                <Skeleton className="h-56 w-full rounded-xl" />
            </div>
        );
    }

    if (isError) {
        return (
            <div className="space-y-4">
                <Button type="button" variant="ghost" className="gap-1.5 px-0 text-muted-foreground" asChild>
                    <Link to="..">
                        <ArrowLeftIcon className="size-4" aria-hidden />
                        Back to Templates
                    </Link>
                </Button>
                <div className="flex flex-col items-start gap-3 rounded-lg border p-6">
                    <p className="text-sm text-muted-foreground">
                        Failed to load this notification template. Please refresh and try again.
                    </p>
                    <Button type="button" variant="outline" size="sm" onClick={() => void refetch()}>
                        Try again
                    </Button>
                </div>
            </div>
        );
    }

    if (!first) {
        return (
            <div className="space-y-4">
                <Button type="button" variant="ghost" className="gap-1.5 px-0 text-muted-foreground" asChild>
                    <Link to="..">
                        <ArrowLeftIcon className="size-4" aria-hidden />
                        Back to Templates
                    </Link>
                </Button>
                <p className="text-sm text-muted-foreground">This notification template no longer exists.</p>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <Button type="button" variant="ghost" className="gap-1.5 px-0 text-muted-foreground" asChild>
                <Link to="..">
                    <ArrowLeftIcon className="size-4" aria-hidden />
                    Back to Templates
                </Link>
            </Button>

            <div className="space-y-2">
                <div className="flex flex-wrap items-center gap-2">
                    <h1 className="text-2xl font-semibold tracking-tight">{first.name}</h1>
                    <Badge variant="outline">{scopeLabel(first.scope)}</Badge>
                    {orderedTemplates.map(template => (
                        <Badge key={template.type} variant="secondary">
                            {template.type === 'EMAIL' ? 'Email' : 'Portal'}
                        </Badge>
                    ))}
                </div>
                {first.description ? <p className="text-sm text-muted-foreground">{first.description}</p> : null}
            </div>

            {isInclude ? (
                <Alert>
                    <InfoIcon className="size-4" aria-hidden />
                    <AlertDescription>
                        To include this template in another template, use <code>{`<#include "${first.name}" />`}</code>
                    </AlertDescription>
                </Alert>
            ) : null}

            {!canEdit ? (
                <Alert>
                    <InfoIcon className="size-4" aria-hidden />
                    <AlertDescription>
                        You have read-only access to notification templates, so this template cannot be changed.
                    </AlertDescription>
                </Alert>
            ) : null}

            {orderedTemplates.map(template => (
                <NotificationTemplateChannelCard
                    key={template.type}
                    type={template.type}
                    draft={draftFor(template)}
                    isInclude={isInclude}
                    disabled={!canPersistChannel(template, canCreate, canUpdate)}
                    showErrors={showErrors}
                    onChange={next => handleDraftChange(template.type, next)}
                />
            ))}

            {isDirty && canEdit ? (
                <div className="sticky bottom-0 z-10 flex flex-wrap items-center justify-end gap-2 border-t bg-background py-4">
                    <p className="mr-auto text-sm text-muted-foreground">You have unsaved changes.</p>
                    <Button variant="outline" size="sm" onClick={() => setDrafts(savedDrafts)} disabled={saveMutation.isPending}>
                        Discard
                    </Button>
                    <Button size="sm" onClick={() => void handleSave()} disabled={saveMutation.isPending}>
                        <CheckIcon className="size-4" aria-hidden />
                        {saveMutation.isPending ? 'Saving…' : 'Save changes'}
                    </Button>
                </div>
            ) : null}
        </div>
    );
}
