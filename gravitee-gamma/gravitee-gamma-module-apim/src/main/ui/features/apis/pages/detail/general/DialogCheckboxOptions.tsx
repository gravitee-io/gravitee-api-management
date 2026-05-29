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
import { Checkbox, Label } from '@gravitee/graphene-core';

export function DialogCheckboxOptions<T extends string>({
    idPrefix,
    options,
    values,
    onChange,
}: Readonly<{
    idPrefix: string;
    options: ReadonlyArray<{ readonly id: T; readonly label: string }>;
    values: Record<T, boolean>;
    onChange: (id: T, checked: boolean) => void;
}>) {
    return (
        <div className="flex flex-col gap-2 pl-1">
            {options.map(option => (
                <div key={option.id} className="flex items-center gap-2 py-0.5">
                    <Checkbox
                        id={`${idPrefix}-${option.id}`}
                        checked={values[option.id]}
                        onCheckedChange={checked => onChange(option.id, checked === true)}
                    />
                    <Label htmlFor={`${idPrefix}-${option.id}`} className="text-sm font-normal cursor-pointer">
                        {option.label}
                    </Label>
                </div>
            ))}
        </div>
    );
}
