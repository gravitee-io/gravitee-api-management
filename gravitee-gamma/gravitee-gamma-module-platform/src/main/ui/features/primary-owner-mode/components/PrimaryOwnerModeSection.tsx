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

import { Card, CardContent, CardDescription, CardHeader, CardTitle, cn, Label, RadioGroup, RadioGroupItem } from '@gravitee/graphene-core';

import { SystemReadonlyHint } from '../../organization-settings/components/SystemReadonlyHint';
import {
    isPrimaryOwnerMode,
    primaryOwnerModeOptions,
    type PrimaryOwnerMode,
    type PrimaryOwnerModeFormState,
    type PrimaryOwnerModeOption,
    type PrimaryOwnerModeReadonly,
    type PrimaryOwnerResource,
} from '../utils/primaryOwnerMode';

function ModeOption({
    id,
    option,
    disabled,
}: Readonly<{
    id: string;
    option: PrimaryOwnerModeOption;
    disabled: boolean;
}>) {
    return (
        <div className="flex items-start gap-3 px-4 py-4">
            <RadioGroupItem value={option.value} id={id} className="mt-0.5" />
            <div className="min-w-0 flex-1 space-y-0.5">
                <Label htmlFor={id} className={cn('text-sm font-medium', disabled ? 'cursor-default' : 'cursor-pointer')}>
                    {option.title}
                </Label>
                <p className="text-xs text-muted-foreground">{option.summary}</p>
                <p className="text-xs text-muted-foreground">{option.implication}</p>
            </div>
        </div>
    );
}

function PrimaryOwnerModeCard({
    title,
    description,
    resource,
    value,
    disabled,
    systemReadonly,
    onChange,
}: Readonly<{
    title: string;
    description: string;
    resource: PrimaryOwnerResource;
    value: PrimaryOwnerMode | null;
    disabled: boolean;
    systemReadonly: boolean;
    onChange: (value: PrimaryOwnerMode) => void;
}>) {
    const idPrefix = resource === 'API' ? 'api-primary-owner' : 'api-product-primary-owner';
    const titleId = `${idPrefix}-title`;
    const options = primaryOwnerModeOptions(resource);

    return (
        <Card>
            <CardHeader>
                <CardTitle id={titleId} className="text-base">
                    {title}
                </CardTitle>
                <CardDescription>{description}</CardDescription>
            </CardHeader>
            <CardContent className="p-0">
                <SystemReadonlyHint locked={systemReadonly}>
                    <RadioGroup
                        value={value ?? ''}
                        onValueChange={next => {
                            if (!isPrimaryOwnerMode(next) || next === value) return;
                            onChange(next);
                        }}
                        disabled={disabled}
                        className="gap-0 divide-y divide-border"
                        aria-labelledby={titleId}
                    >
                        {options.map(option => (
                            <ModeOption
                                key={option.value}
                                id={`${idPrefix}-${option.value.toLowerCase()}`}
                                option={option}
                                disabled={disabled}
                            />
                        ))}
                    </RadioGroup>
                </SystemReadonlyHint>
            </CardContent>
        </Card>
    );
}

export function PrimaryOwnerModeSection({
    value,
    disabled,
    readonly,
    onChange,
}: Readonly<{
    value: PrimaryOwnerModeFormState;
    disabled: boolean;
    readonly: PrimaryOwnerModeReadonly;
    onChange: (next: PrimaryOwnerModeFormState) => void;
}>) {
    return (
        <div className="space-y-6">
            <PrimaryOwnerModeCard
                title="API Primary Owner mode"
                description="Who can be the primary owner of an API created in this environment."
                resource="API"
                value={value.api}
                disabled={disabled || readonly.api}
                systemReadonly={readonly.api}
                onChange={api => onChange({ ...value, api })}
            />
            <PrimaryOwnerModeCard
                title="API Product Primary Owner mode"
                description="Who can be the primary owner of an API product created in this environment."
                resource="API Product"
                value={value.apiProduct}
                disabled={disabled || readonly.apiProduct}
                systemReadonly={readonly.apiProduct}
                onChange={apiProduct => onChange({ ...value, apiProduct })}
            />
        </div>
    );
}
