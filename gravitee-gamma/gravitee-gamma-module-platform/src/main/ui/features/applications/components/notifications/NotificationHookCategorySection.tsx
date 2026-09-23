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
import { Checkbox, Input } from '@gravitee/graphene-core';

import type { ApplicationNotificationHookCategory } from '../../types/applicationNotification';
import {
    CERTIFICATE_CLOSE_TO_EXPIRY_HOOK_ID,
    CLOSE_TO_EXPIRY_HOOK_IDS,
    MAX_CLOSE_TO_EXPIRY_DAYS,
    MIN_CLOSE_TO_EXPIRY_DAYS,
} from '../../utils/applicationNotificationHooks';

export function NotificationHookCategorySection({
    category,
    selectedHooks,
    groupHookIds,
    disabled,
    closeToExpiryDaysByHookId,
    onToggle,
    onCloseToExpiryDaysChange,
}: Readonly<{
    category: ApplicationNotificationHookCategory;
    selectedHooks: Set<string>;
    groupHookIds: Set<string>;
    disabled: boolean;
    closeToExpiryDaysByHookId: Record<string, number>;
    onToggle: (hookId: string) => void;
    onCloseToExpiryDaysChange: (hookId: string, days: number) => void;
}>) {
    return (
        <div className="space-y-2">
            <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">{category.name}</p>
            <div className="grid gap-2 sm:grid-cols-2 xl:grid-cols-4">
                {category.hooks.map(hook => {
                    const isGroupHook = groupHookIds.has(hook.id);
                    const isDisabled = disabled || isGroupHook;
                    const showDays = CLOSE_TO_EXPIRY_HOOK_IDS.has(hook.id) && selectedHooks.has(hook.id);
                    const days = closeToExpiryDaysByHookId[hook.id];
                    return (
                        <div key={hook.id} className="rounded-md p-2">
                            <label className="flex items-start gap-2">
                                <Checkbox
                                    checked={selectedHooks.has(hook.id)}
                                    onCheckedChange={checked => checked !== 'indeterminate' && !isDisabled && onToggle(hook.id)}
                                    disabled={isDisabled}
                                    className="mt-0.5 shrink-0"
                                />
                                <span className="min-w-0">
                                    <span className="block text-sm font-medium leading-snug">{hook.label}</span>
                                    {hook.description ? (
                                        <span className="block text-xs text-muted-foreground">{hook.description}</span>
                                    ) : null}
                                </span>
                            </label>
                            {showDays ? (
                                <div className="ml-7 mt-2 flex items-center gap-2">
                                    <Input
                                        id={`close-to-expiry-days-${hook.id}`}
                                        type="number"
                                        min={MIN_CLOSE_TO_EXPIRY_DAYS}
                                        max={MAX_CLOSE_TO_EXPIRY_DAYS}
                                        value={Number.isFinite(days) ? days : ''}
                                        onChange={event => onCloseToExpiryDaysChange(hook.id, Number(event.target.value))}
                                        disabled={isDisabled}
                                        className="w-20"
                                        aria-label={`Days before ${hook.id === CERTIFICATE_CLOSE_TO_EXPIRY_HOOK_ID ? 'certificate' : 'subscription'} expiry`}
                                    />
                                    <span className="text-xs text-muted-foreground">days before expiry</span>
                                </div>
                            ) : null}
                        </div>
                    );
                })}
            </div>
        </div>
    );
}
