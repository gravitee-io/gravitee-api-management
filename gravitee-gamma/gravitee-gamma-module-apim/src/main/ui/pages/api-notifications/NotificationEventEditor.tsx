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
import { Button, Card, CardContent, CardHeader, CardTitle, Checkbox, Input, Label, Separator, Skeleton } from '@gravitee/graphene-core';
import { Fragment, useCallback, useMemo, useState } from 'react';

import type { HookCategory, NotificationRow } from '../../features/apis/hooks/useApiNotifications';

// ─── Loading skeleton ─────────────────────────────────────────────────────────

function EditorSkeleton() {
    return (
        <div className="space-y-4">
            {[1, 2, 3].map(i => (
                <div key={i} className="space-y-2">
                    <Skeleton className="h-4 w-24 rounded" />
                    <div className="grid grid-cols-2 gap-2">
                        {[1, 2, 3, 4].map(j => (
                            <Skeleton key={j} className="h-5 w-full rounded" />
                        ))}
                    </div>
                </div>
            ))}
        </div>
    );
}

// ─── Category section ─────────────────────────────────────────────────────────

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
            <div className="grid grid-cols-2 gap-1.5">
                {category.hooks.map(hook => {
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

// ─── Props ────────────────────────────────────────────────────────────────────

interface NotificationEventEditorProps {
    row: NotificationRow;
    hookCategories: HookCategory[];
    isLoadingHooks: boolean;
    isPending: boolean;
    onSave: (hooks: string[], config: string) => void;
    onCancel: () => void;
}

// ─── Component ────────────────────────────────────────────────────────────────

export function NotificationEventEditor({
    row,
    hookCategories,
    isLoadingHooks,
    isPending,
    onSave,
    onCancel,
}: Readonly<NotificationEventEditorProps>) {
    const groupHookIds = useMemo(() => new Set(row.notification.groupHooks ?? []), [row.notification.groupHooks]);

    // Initial state only — parent passes key={row.key} so component remounts on row change
    const [selectedHooks, setSelectedHooks] = useState<Set<string>>(
        new Set([...(row.notification.hooks ?? []), ...(row.notification.groupHooks ?? [])]),
    );
    const [config, setConfig] = useState(row.notification.config ?? '');

    const needsTarget = row.channel === 'EMAIL' || row.channel === 'WEBHOOK';
    const targetLabel = row.channel === 'EMAIL' ? 'Email address(es)' : 'Webhook URL';
    const targetPlaceholder = row.channel === 'EMAIL' ? 'e.g. user@example.com' : 'e.g. https://hooks.example.com/notify';

    const toggleHook = useCallback((hookId: string) => {
        setSelectedHooks(prev => {
            const next = new Set(prev);
            if (next.has(hookId)) next.delete(hookId);
            else next.add(hookId);
            return next;
        });
    }, []);

    const handleSave = useCallback(() => {
        // Strip groupHooks before saving — they are not user-owned
        const userHooks = [...selectedHooks].filter(h => !groupHookIds.has(h));
        onSave(userHooks, config);
    }, [onSave, selectedHooks, groupHookIds, config]);

    const isReadonly = row.isReadonly;

    return (
        <Card style={{ border: '2px solid color-mix(in oklab, var(--color-primary) 25%, transparent)' }}>
            <CardHeader className="pb-2">
                <CardTitle className="text-sm">
                    Edit events — <span className="font-normal text-muted-foreground">{row.notification.name}</span>
                </CardTitle>
            </CardHeader>
            <CardContent className="space-y-5">
                {/* Target field (email / webhook) */}
                {needsTarget && (
                    <>
                        <div className="space-y-2">
                            <Label htmlFor="editor-target">{targetLabel}</Label>
                            <Input
                                id="editor-target"
                                value={config}
                                onChange={e => setConfig(e.target.value)}
                                placeholder={targetPlaceholder}
                                disabled={isReadonly || isPending}
                            />
                        </div>
                        <Separator />
                    </>
                )}

                {/* Event checkboxes */}
                {isLoadingHooks ? (
                    <EditorSkeleton />
                ) : (
                    <div className="space-y-4">
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

                {/* Actions */}
                <div className="flex items-center justify-end gap-3 pt-2">
                    <Button type="button" variant="outline" size="sm" onClick={onCancel} disabled={!isReadonly && isPending}>
                        {isReadonly ? 'Close' : 'Cancel'}
                    </Button>
                    {!isReadonly && (
                        <Button type="button" size="sm" onClick={handleSave} disabled={isPending || isLoadingHooks}>
                            {isPending ? 'Saving…' : 'Save events'}
                        </Button>
                    )}
                </div>
            </CardContent>
        </Card>
    );
}
