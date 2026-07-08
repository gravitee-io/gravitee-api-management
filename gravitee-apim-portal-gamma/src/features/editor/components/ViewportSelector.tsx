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

import type { PreviewViewport } from '../constants/preview-viewport';

const viewportOptions: { value: PreviewViewport; label: string; icon: React.ReactNode }[] = [
    {
        value: 'desktop',
        label: 'Desktop',
        icon: (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                <rect x="2" y="3" width="20" height="14" rx="2" />
                <path d="M8 21h8" />
                <path d="M12 17v4" />
            </svg>
        ),
    },
    {
        value: 'tablet',
        label: 'Tablet',
        icon: (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                <rect x="5" y="2" width="14" height="20" rx="2" />
                <path d="M12 18h.01" />
            </svg>
        ),
    },
    {
        value: 'mobile',
        label: 'Mobile',
        icon: (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                <rect x="7" y="2" width="10" height="20" rx="2" />
                <path d="M12 18h.01" />
            </svg>
        ),
    },
];

interface ViewportSelectorProps {
    readonly value: PreviewViewport;
    readonly onChange: (value: PreviewViewport) => void;
}

export function ViewportSelector({ value, onChange }: ViewportSelectorProps) {
    return (
        <ToggleGroup
            type="single"
            variant="outline"
            size="sm"
            spacing={0}
            value={value}
            onValueChange={nextValue => {
                if (nextValue === 'desktop' || nextValue === 'tablet' || nextValue === 'mobile') {
                    onChange(nextValue);
                }
            }}
            aria-label="Preview viewport"
        >
            {viewportOptions.map(option => (
                <ToggleGroupItem key={option.value} value={option.value} aria-label={option.label} title={option.label}>
                    {option.icon}
                </ToggleGroupItem>
            ))}
        </ToggleGroup>
    );
}
