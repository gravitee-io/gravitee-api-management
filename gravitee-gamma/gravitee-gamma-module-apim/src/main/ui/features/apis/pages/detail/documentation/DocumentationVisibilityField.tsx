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
import { Field, FieldDescription, FieldLabel, Switch } from '@gravitee/graphene-core';

import type { Visibility } from '../../../types/documentation';

/**
 * Same auth toggle as Next Gen Portal Navigation create/edit dialogs:
 * when on, the item is PRIVATE (authenticated users only).
 * Public is disabled when the parent folder already requires authentication.
 */
export function DocumentationVisibilityField({
    kind,
    value,
    disabled,
    parentForcesPrivate,
    onChange,
}: {
    readonly kind: 'page' | 'folder';
    readonly value: Visibility;
    readonly disabled?: boolean;
    readonly parentForcesPrivate?: boolean;
    readonly onChange: (value: Visibility) => void;
}) {
    const locked = Boolean(disabled || parentForcesPrivate);
    const isPrivate = value === 'PRIVATE';

    return (
        <Field>
            <FieldLabel>Authentication</FieldLabel>
            <div className="flex items-center justify-between gap-4 rounded-lg border px-4 py-3">
                <div className="min-w-0 space-y-1">
                    <p className="text-sm font-medium">Authentication is required to view this {kind}.</p>
                    {parentForcesPrivate ? (
                        <FieldDescription>
                            This {kind} is in a folder requiring authentication, so it must stay private.
                        </FieldDescription>
                    ) : (
                        <FieldDescription>
                            When enabled, only signed-in users can see this {kind} in the Developer Portal.
                        </FieldDescription>
                    )}
                </div>
                <Switch
                    checked={isPrivate}
                    disabled={locked}
                    aria-label="Require authentication"
                    onCheckedChange={checked => {
                        if (locked) return;
                        onChange(checked ? 'PRIVATE' : 'PUBLIC');
                    }}
                />
            </div>
        </Field>
    );
}
