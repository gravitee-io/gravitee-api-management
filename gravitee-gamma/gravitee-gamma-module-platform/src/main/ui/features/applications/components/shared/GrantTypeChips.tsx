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
import { Badge, Button, cn } from '@gravitee/graphene-core';
import { CircleCheckIcon } from '@gravitee/graphene-core/icons';

import type { ApplicationTypeConfig } from '../../types/applicationCreate';
import { isMandatoryGrantType } from '../../utils/applicationCreateMapper';

export interface GrantTypeChipsProps {
    readonly typeConfig: ApplicationTypeConfig;
    readonly values: string[];
    readonly onChange: (next: string[]) => void;
    readonly disabled?: boolean;
    /** When true (default), active mandatory grant types cannot be deselected (Create flow). */
    readonly lockMandatoryGrantTypes?: boolean;
}

export function GrantTypeChips({ typeConfig, values, onChange, disabled, lockMandatoryGrantTypes = true }: GrantTypeChipsProps) {
    const toggle = (grantType: string) => {
        if (disabled) {
            return;
        }
        const isSelected = values.includes(grantType);
        if (lockMandatoryGrantTypes && isSelected && isMandatoryGrantType(typeConfig, grantType)) {
            return;
        }
        onChange(isSelected ? values.filter(value => value !== grantType) : [...values, grantType]);
    };

    return (
        <div className="flex flex-wrap gap-2">
            {typeConfig.allowed_grant_types.map(grantType => {
                const isActive = values.includes(grantType.type);
                const isMandatory = isMandatoryGrantType(typeConfig, grantType.type);
                const isLockedMandatory = lockMandatoryGrantTypes && isMandatory && isActive;

                return (
                    <Button
                        key={grantType.type}
                        type="button"
                        variant={isActive ? 'default' : 'outline'}
                        size="sm"
                        disabled={disabled || isLockedMandatory}
                        aria-pressed={isActive}
                        onClick={() => toggle(grantType.type)}
                        className={cn(
                            'h-auto gap-1.5 px-3 py-2 font-normal',
                            isActive && 'border-primary bg-primary/10 text-primary hover:bg-primary/15',
                        )}
                    >
                        {isActive && <CircleCheckIcon className="size-3.5" aria-hidden />}
                        {grantType.name}
                        {isMandatory ? (
                            <Badge variant="secondary" className="ml-1 text-xs">
                                Mandatory
                            </Badge>
                        ) : null}
                    </Button>
                );
            })}
        </div>
    );
}
