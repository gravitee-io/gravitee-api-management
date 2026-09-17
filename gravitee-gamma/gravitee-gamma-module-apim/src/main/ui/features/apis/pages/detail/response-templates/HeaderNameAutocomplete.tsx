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

import { useMemo } from 'react';

import { FreeTextAutocomplete } from '../../../../../shared/components/FreeTextAutocomplete';
import { filterHttpHeaderNames } from '../../../utils/httpHeaderNames';

type HeaderNameAutocompleteProps = {
    value: string;
    disabled?: boolean;
    onChange: (value: string) => void;
    showInvalid?: boolean;
};

export function isValidHttpHeaderName(name: string): boolean {
    return name.length === 0 || /^\S*$/.test(name);
}

export function HeaderNameAutocomplete({ value, disabled, onChange, showInvalid = true }: Readonly<HeaderNameAutocompleteProps>) {
    const suggestions = useMemo(() => filterHttpHeaderNames(value), [value]);

    return (
        <FreeTextAutocomplete
            className="flex-1 space-y-1"
            value={value}
            onChange={onChange}
            suggestions={suggestions}
            disabled={disabled}
            placeholder="Header name"
            aria-label="Header name"
            emptyMessage="No matching headers — your custom value will be used."
            isValid={isValidHttpHeaderName}
            invalidMessage="Header name must not contain spaces."
            showInvalid={showInvalid}
        />
    );
}
