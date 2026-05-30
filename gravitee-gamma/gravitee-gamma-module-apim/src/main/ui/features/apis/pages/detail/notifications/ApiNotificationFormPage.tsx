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
import {
    Badge,
    Button,
    Card,
    CardContent,
    Checkbox,
    Input,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Separator,
    Skeleton,
    Switch,
} from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { Fragment } from 'react';
import { Navigate } from 'react-router-dom';

import { useApiNotificationForm } from './useApiNotificationForm';
import type { HookCategory } from '../../../hooks/useApiNotifications';
import { CHANNEL_ICON, CHANNEL_LABEL } from '../../../utils/notificationFormatters';

// ─── Event category section ───────────────────────────────────────────────────

interface CategorySectionProps {
    category: HookCategory;
    selected: Set<string>;
    groupHookIds: Set<string>;
    onToggle: (hookId: string) => void;
    readonly: boolean;
}

function CategorySection({ category, selected, groupHookIds, onToggle, readonly }: Readonly<CategorySectionProps>) {
    return (
        <div className="space-y-2">
            <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">{category.name}</p>
            <div className="grid grid-cols-1 gap-1.5 sm:grid-cols-2">
                {category.hooks.map(hook => {
                    // Group-inherited hooks are shown for context but cannot be toggled.
                    const isGroupHook = groupHookIds.has(hook.id);
                    const isDisabled = readonly || isGroupHook;
                    return (
                        <label
                            key={hook.id}
                            className="flex items-start gap-2 rounded-md p-2 transition-colors"
                            style={isDisabled ? { opacity: 0.6 } : { cursor: 'pointer' }}
                        >
                            <Checkbox
                                checked={selected.has(hook.id)}
                                onCheckedChange={() => !isDisabled && onToggle(hook.id)}
                                disabled={isDisabled}
                                className="mt-0.5 shrink-0"
                            />
                            <div className="min-w-0">
                                <p className="text-sm font-medium leading-snug">{hook.label}</p>
                                {hook.description ? <p className="text-xs text-muted-foreground">{hook.description}</p> : null}
                            </div>
                        </label>
                    );
                })}
            </div>
        </div>
    );
}

function EventsSkeleton() {
    return (
        <div className="space-y-4">
            {[1, 2, 3].map(i => (
                <div key={i} className="space-y-2">
                    <Skeleton className="h-4 w-24 rounded" />
                    <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                        {[1, 2, 3, 4].map(j => (
                            <Skeleton key={j} className="h-5 w-full rounded" />
                        ))}
                    </div>
                </div>
            ))}
        </div>
    );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export function ApiNotificationFormPage() {
    const {
        isUpdate,
        allowed,
        isReadonly,
        notFound,
        isLoading,
        isLoadingHooks,
        isPending,
        saveError,
        name,
        setName,
        nameReadonly,
        channel,
        channelOptions,
        selectedNotifierId,
        setSelectedNotifierId,
        needsTarget,
        config,
        setConfig,
        showSystemProxy,
        useSystemProxy,
        setUseSystemProxy,
        hookCategories,
        groupHookIds,
        selectedHooks,
        toggleHook,
        canSubmit,
        handleSave,
        handleCancel,
    } = useApiNotificationForm();

    // Guard: lacking the required permission for this screen → back to the list.
    if (!allowed) return <Navigate to=".." replace />;

    // Edit mode still resolving the target notification.
    if (isUpdate && isLoading) {
        return (
            <div className="space-y-6 p-6">
                <Skeleton className="h-8 w-48 rounded" />
                <Skeleton className="h-64 w-full rounded-lg" />
            </div>
        );
    }

    // Edit mode, data loaded, notification no longer exists.
    if (notFound) return <Navigate to=".." replace />;

    const ChannelIcon = CHANNEL_ICON[channel];
    const targetLabel = channel === 'EMAIL' ? 'Email address(es)' : 'Webhook URL';
    const targetPlaceholder = channel === 'EMAIL' ? 'e.g. ops@example.com' : 'e.g. https://hooks.example.com/notify';

    // Add mode with no email/webhook channels configured for this API.
    const noChannelsForAdd = !isUpdate && channelOptions.length === 0;

    return (
        <div className="space-y-6 p-6">
            {/* ─── Header ─────────────────────────────────────────────────── */}
            <div>
                <Button variant="ghost" size="sm" className="-ml-2 mb-3 text-muted-foreground" onClick={handleCancel}>
                    <ArrowLeftIcon className="size-4" />
                    Back to notifications
                </Button>
                <h1 className="text-2xl font-semibold tracking-tight">{isUpdate ? 'Edit notification' : 'Add notification'}</h1>
                <p className="mt-1 text-sm text-muted-foreground">
                    Choose a channel, then pick the API events that should trigger an alert.
                </p>
            </div>

            {/* ─── Save error ──────────────────────────────────────────────── */}
            {saveError && (
                <div className="rounded-lg border border-destructive/30 bg-destructive/5 px-4 py-3">
                    <p className="text-sm text-destructive">{saveError}</p>
                </div>
            )}

            {noChannelsForAdd ? (
                <Card>
                    <CardContent className="p-6">
                        <p className="text-sm text-muted-foreground">
                            No email or webhook channels are configured for this API. Console notifications can be edited from the
                            notifications list.
                        </p>
                    </CardContent>
                </Card>
            ) : (
                <Card>
                    <CardContent className="space-y-6 pt-6">
                        {/* ── Name & Channel ─────────────────────────────────── */}
                        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                            <div className="space-y-2">
                                <Label htmlFor="notif-name">Name {!isUpdate && <span className="text-destructive">*</span>}</Label>
                                <Input
                                    id="notif-name"
                                    value={name}
                                    onChange={e => setName(e.target.value)}
                                    placeholder="e.g. Ops webhook"
                                    disabled={nameReadonly || isReadonly || isPending}
                                    required={!isUpdate}
                                />
                            </div>

                            <div className="space-y-2">
                                <Label htmlFor={isUpdate ? undefined : 'notif-channel'}>Channel</Label>
                                {isUpdate ? (
                                    <div>
                                        <Badge variant="outline" className="gap-1">
                                            <ChannelIcon className="size-3" />
                                            {CHANNEL_LABEL[channel]}
                                        </Badge>
                                    </div>
                                ) : (
                                    <Select value={selectedNotifierId} onValueChange={setSelectedNotifierId} disabled={isPending}>
                                        <SelectTrigger id="notif-channel" className="w-full">
                                            <SelectValue />
                                        </SelectTrigger>
                                        <SelectContent>
                                            {channelOptions.map(opt => {
                                                const Icon = CHANNEL_ICON[opt.type];
                                                return (
                                                    <SelectItem key={opt.notifierId} value={opt.notifierId}>
                                                        <span className="flex items-center gap-2">
                                                            <Icon className="size-3.5" />
                                                            {opt.label}
                                                        </span>
                                                    </SelectItem>
                                                );
                                            })}
                                        </SelectContent>
                                    </Select>
                                )}
                            </div>
                        </div>

                        {/* ── Target (email / webhook) ───────────────────────── */}
                        {needsTarget && (
                            <div className="space-y-2">
                                <Label htmlFor="notif-target">{targetLabel}</Label>
                                <Input
                                    id="notif-target"
                                    value={config}
                                    onChange={e => setConfig(e.target.value)}
                                    placeholder={targetPlaceholder}
                                    disabled={isReadonly || isPending}
                                />
                            </div>
                        )}

                        {/* ── Use system proxy (webhook only) ────────────────── */}
                        {showSystemProxy && (
                            <div className="flex items-center justify-between gap-4 rounded-lg border bg-muted/40 px-4 py-3">
                                <div>
                                    <p className="text-sm font-medium">Use system proxy</p>
                                    <p className="text-xs text-muted-foreground">
                                        Route webhook calls through the gateway&apos;s configured system proxy.
                                    </p>
                                </div>
                                <Switch
                                    checked={useSystemProxy}
                                    onCheckedChange={setUseSystemProxy}
                                    disabled={isReadonly || isPending}
                                    aria-label="Use system proxy"
                                />
                            </div>
                        )}

                        <Separator />

                        {/* ── Events ─────────────────────────────────────────── */}
                        <div className="space-y-2">
                            <div className="space-y-1">
                                <p className="text-sm font-medium">Events</p>
                                <p className="text-xs text-muted-foreground">Select the API events that should send a notification.</p>
                            </div>
                            {isLoadingHooks ? (
                                <EventsSkeleton />
                            ) : (
                                <div className="space-y-4 pt-1">
                                    {hookCategories.map((cat, idx) => (
                                        <Fragment key={cat.name}>
                                            {idx > 0 && <Separator />}
                                            <CategorySection
                                                category={cat}
                                                selected={selectedHooks}
                                                groupHookIds={groupHookIds}
                                                onToggle={toggleHook}
                                                readonly={isReadonly}
                                            />
                                        </Fragment>
                                    ))}
                                </div>
                            )}
                        </div>
                    </CardContent>
                </Card>
            )}

            {/* ─── Footer actions ──────────────────────────────────────────── */}
            <div className="flex items-center justify-end gap-3">
                <Button type="button" variant="outline" onClick={handleCancel} disabled={!isReadonly && isPending}>
                    {isReadonly ? 'Close' : 'Cancel'}
                </Button>
                {!isReadonly && !noChannelsForAdd && (
                    <Button type="button" onClick={handleSave} disabled={!canSubmit}>
                        {isPending ? 'Saving…' : isUpdate ? 'Save' : 'Add notification'}
                    </Button>
                )}
            </div>
        </div>
    );
}
