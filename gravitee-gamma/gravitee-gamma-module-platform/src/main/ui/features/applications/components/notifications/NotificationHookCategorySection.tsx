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
import { Checkbox } from '@gravitee/graphene-core';

import type { ApplicationNotificationHookCategory } from '../../types/applicationNotification';

export function NotificationHookCategorySection({
    category,
    selectedHooks,
    groupHookIds,
    disabled,
    onToggle,
}: Readonly<{
    category: ApplicationNotificationHookCategory;
    selectedHooks: Set<string>;
    groupHookIds: Set<string>;
    disabled: boolean;
    onToggle: (hookId: string) => void;
}>) {
    return (
        <div className="space-y-2">
            <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">{category.name}</p>
            <div className="grid gap-2 sm:grid-cols-2 xl:grid-cols-4">
                {category.hooks.map(hook => {
                    const isGroupHook = groupHookIds.has(hook.id);
                    const isDisabled = disabled || isGroupHook;
                    return (
                        <label key={hook.id} className="flex items-start gap-2 rounded-md p-2">
                            <Checkbox
                                checked={selectedHooks.has(hook.id)}
                                onCheckedChange={checked => checked !== 'indeterminate' && !isDisabled && onToggle(hook.id)}
                                disabled={isDisabled}
                                className="mt-0.5 shrink-0"
                            />
                            <span className="min-w-0">
                                <span className="block text-sm font-medium leading-snug">{hook.label}</span>
                                {hook.description ? <span className="block text-xs text-muted-foreground">{hook.description}</span> : null}
                            </span>
                        </label>
                    );
                })}
            </div>
        </div>
    );
}
