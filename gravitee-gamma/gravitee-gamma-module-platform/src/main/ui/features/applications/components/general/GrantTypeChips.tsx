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
import { Badge, cn } from '@gravitee/graphene-core';

import type { ApplicationGrantType, ApplicationTypeConfig } from '../../types/applicationCreate';
import { isMandatoryGrantType } from '../../utils/applicationCreateMapper';

export function GrantTypeChips({
    typeConfig,
    options,
    values,
    onChange,
    disabled,
}: Readonly<{
    typeConfig: ApplicationTypeConfig;
    options: ApplicationGrantType[];
    values: string[];
    onChange: (next: string[]) => void;
    disabled?: boolean;
}>) {
    const toggle = (grantType: string) => {
        if (disabled || isMandatoryGrantType(typeConfig, grantType)) return;
        onChange(values.includes(grantType) ? values.filter(v => v !== grantType) : [...values, grantType]);
    };

    return (
        <div className="flex flex-wrap gap-1.5">
            {options.map(gt => {
                const isMandatory = isMandatoryGrantType(typeConfig, gt.type);
                const isChipDisabled = disabled || isMandatory;

                return (
                    <button
                        key={gt.type}
                        type="button"
                        disabled={isChipDisabled}
                        onClick={() => toggle(gt.type)}
                        className={cn(
                            'inline-flex h-6 items-center gap-1 border px-2 text-xs transition-colors disabled:opacity-50',
                            values.includes(gt.type)
                                ? 'border-primary bg-primary text-primary-foreground'
                                : 'border-border bg-background text-foreground hover:border-primary/40',
                            isMandatory && !disabled && 'cursor-not-allowed',
                        )}
                    >
                        {gt.name}
                        {isMandatory ? (
                            <Badge variant="secondary" className="ml-1 px-1 py-0 text-xs leading-none">
                                Mandatory
                            </Badge>
                        ) : null}
                    </button>
                );
            })}
        </div>
    );
}
