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
import { ChipInputField } from '@gravitee/gamma-ui-shared/chip-input';
import { Label } from '@gravitee/graphene-core';
import type { ReactNode } from 'react';

import { InfoTooltip } from './InfoTooltip';

export interface ChipsProps {
    label: string;
    hint: ReactNode;
    values: string[];
    placeholder: string;
    disabled?: boolean;
    suggestions?: readonly string[];
    /** When false, blur discards the draft (Classic CORS headers). Default true. */
    addOnBlur?: boolean;
    onChange: (next: string[]) => void;
}

export function Chips({ label, hint, values, placeholder, disabled, suggestions, addOnBlur = true, onChange }: ChipsProps) {
    return (
        <div className="space-y-2">
            <div className="flex items-center gap-1.5">
                <Label className={disabled ? 'text-muted-foreground' : ''}>{label}</Label>
                <InfoTooltip content={hint} />
            </div>

            <ChipInputField
                values={values}
                onChange={onChange}
                placeholder={values.length === 0 ? placeholder : ''}
                disabled={disabled}
                suggestions={suggestions}
                addOnBlur={addOnBlur}
                addOnComma
                monospace
                inputAriaLabel={label}
                fieldClassName="gap-2 min-h-11"
                inputClassName="min-w-36 placeholder:text-muted-foreground disabled:cursor-not-allowed"
            />
        </div>
    );
}
