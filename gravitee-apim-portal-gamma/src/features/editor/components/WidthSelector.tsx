/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { ToggleGroup, ToggleGroupItem } from '@gravitee/graphene-core';

import type { PageWidth } from '../constants/page-width';

const widthOptions: { value: PageWidth; label: string; icon: React.ReactNode }[] = [
    {
        value: 'narrow',
        label: 'Narrow',
        icon: (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                <rect x="6" y="3" width="12" height="18" rx="1" />
            </svg>
        ),
    },
    {
        value: 'medium',
        label: 'Medium',
        icon: (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                <rect x="4" y="3" width="16" height="18" rx="1" />
            </svg>
        ),
    },
    {
        value: 'wide',
        label: 'Wide',
        icon: (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                <rect x="2" y="3" width="20" height="18" rx="1" />
            </svg>
        ),
    },
];

interface WidthSelectorProps {
    readonly value: PageWidth;
    readonly onChange: (value: PageWidth) => void;
}

export function WidthSelector({ value, onChange }: WidthSelectorProps) {
    return (
        <ToggleGroup
            type="single"
            variant="outline"
            size="sm"
            spacing={0}
            value={value}
            onValueChange={nextValue => {
                if (nextValue === 'narrow' || nextValue === 'medium' || nextValue === 'wide') {
                    onChange(nextValue);
                }
            }}
            aria-label="Page width"
        >
            {widthOptions.map(option => (
                <ToggleGroupItem key={option.value} value={option.value} aria-label={option.label} title={option.label}>
                    {option.icon}
                </ToggleGroupItem>
            ))}
        </ToggleGroup>
    );
}
