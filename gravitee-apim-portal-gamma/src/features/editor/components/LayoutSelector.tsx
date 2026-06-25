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

import type { PortalLayout } from '../../portals/types';

const layoutOptions: { value: PortalLayout; label: string; icon: React.ReactNode }[] = [
    {
        value: 'header-content-footer',
        label: 'Header, content, footer',
        icon: (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                <rect x="3" y="3" width="18" height="4" rx="1" />
                <rect x="3" y="9" width="18" height="9" rx="1" />
                <rect x="3" y="20" width="18" height="1" rx="0.5" />
            </svg>
        ),
    },
    {
        value: 'sidebar-content',
        label: 'Sidebar and content',
        icon: (
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                <rect x="3" y="3" width="6" height="18" rx="1" />
                <rect x="11" y="3" width="10" height="18" rx="1" />
            </svg>
        ),
    },
];

interface LayoutSelectorProps {
    readonly value: PortalLayout;
    readonly onChange: (value: PortalLayout) => void;
}

export function LayoutSelector({ value, onChange }: LayoutSelectorProps) {
    return (
        <ToggleGroup
            type="single"
            variant="outline"
            size="sm"
            spacing={0}
            value={value}
            onValueChange={nextValue => {
                if (nextValue === 'header-content-footer' || nextValue === 'sidebar-content') {
                    onChange(nextValue);
                }
            }}
            aria-label="Portal layout"
        >
            {layoutOptions.map(option => (
                <ToggleGroupItem key={option.value} value={option.value} aria-label={option.label} title={option.label}>
                    {option.icon}
                </ToggleGroupItem>
            ))}
        </ToggleGroup>
    );
}
