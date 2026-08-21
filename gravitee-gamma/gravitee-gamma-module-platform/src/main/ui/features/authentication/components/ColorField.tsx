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

import { Input } from '@gravitee/graphene-core';
import { useEffect, useState } from 'react';

const HEX_COLOR = /^#([0-9a-fA-F]{6})$/;

export function ColorField({
    id,
    value,
    disabled,
    onChange,
}: {
    readonly id: string;
    readonly value: string;
    readonly disabled: boolean;
    readonly onChange: (value: string) => void;
}) {
    const [hex, setHex] = useState(value);
    const pickerValue = HEX_COLOR.test(value) ? value : '#000000';
    const hexId = `${id}-hex`;

    useEffect(() => {
        setHex(value);
    }, [value]);

    function commitHex() {
        const trimmed = hex.trim();
        if (trimmed === '') {
            onChange('');
            setHex('');
            return;
        }
        if (HEX_COLOR.test(trimmed)) {
            onChange(trimmed);
            setHex(trimmed);
            return;
        }
        setHex(value);
    }

    return (
        <div className="flex items-center gap-2">
            <Input
                id={id}
                type="color"
                value={pickerValue}
                disabled={disabled}
                className="size-9 p-1"
                onChange={event => onChange(event.target.value)}
            />
            <Input
                id={hexId}
                value={hex}
                disabled={disabled}
                placeholder="Select color"
                aria-label="Authentication button color hex value"
                onChange={event => setHex(event.target.value)}
                onBlur={commitHex}
            />
        </div>
    );
}
