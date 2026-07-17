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
import type { SizePreset } from './size-presets';
import type { FoundationTokens } from '../model/theme-document';

export interface PropertyDef {
    readonly type: 'color' | 'size' | 'font-family' | 'font-weight' | 'number';
    readonly label: string;
    readonly sizePresets?: readonly SizePreset[];
    readonly allowCustomValue?: boolean;
    readonly foundationFallback?: keyof FoundationTokens;
}

export interface PartDef {
    readonly id: string;
    readonly label: string;
    readonly properties: Record<string, PropertyDef>;
}

export interface ElementDef {
    readonly id: string;
    readonly label: string;
    readonly variants?: readonly string[];
    readonly parts?: readonly PartDef[];
    readonly properties: Record<string, PropertyDef>;
    readonly category: 'layout' | 'component';
}

const SHARED_PROPERTIES: Record<string, PropertyDef> = {
    background: { type: 'color', label: 'Background' },
    foreground: { type: 'color', label: 'Text color' },
    borderColor: { type: 'color', label: 'Border color' },
    borderWidth: {
        type: 'size',
        label: 'Border width',
        sizePresets: ['xs', 'sm', 'md', 'lg', 'xl'],
        allowCustomValue: true,
    },
    borderRadius: {
        type: 'size',
        label: 'Border radius',
        sizePresets: ['xs', 'sm', 'md', 'lg', 'xl'],
        allowCustomValue: true,
    },
    fontSize: {
        type: 'size',
        label: 'Font size',
        sizePresets: ['xs', 'sm', 'md', 'lg', 'xl'],
        allowCustomValue: true,
    },
    fontWeight: { type: 'font-weight', label: 'Font weight' },
    padding: {
        type: 'size',
        label: 'Padding',
        sizePresets: ['xs', 'sm', 'md', 'lg', 'xl'],
        allowCustomValue: true,
    },
    height: {
        type: 'size',
        label: 'Height',
        sizePresets: ['xs', 'sm', 'md', 'lg', 'xl'],
        allowCustomValue: true,
    },
};

const NAV_ITEM_DEFAULT_PROPERTIES: Record<string, PropertyDef> = {
    background: { ...SHARED_PROPERTIES.background, foundationFallback: 'background' },
    foreground: { ...SHARED_PROPERTIES.foreground, foundationFallback: 'text' },
    fontSize: { ...SHARED_PROPERTIES.fontSize, foundationFallback: 'fontSize' },
    padding: { ...SHARED_PROPERTIES.padding, foundationFallback: 'padding' },
    borderRadius: { ...SHARED_PROPERTIES.borderRadius, foundationFallback: 'borderRadius' },
};

const NAV_ITEM_SELECTED_PROPERTIES: Record<string, PropertyDef> = {
    background: { ...SHARED_PROPERTIES.background, foundationFallback: 'muted' },
    foreground: { ...SHARED_PROPERTIES.foreground, foundationFallback: 'text' },
    fontWeight: SHARED_PROPERTIES.fontWeight,
};

export const POC_ELEMENT_REGISTRY: readonly ElementDef[] = [
    {
        id: 'button',
        label: 'Button',
        category: 'component',
        variants: ['filled', 'outlined'],
        properties: {
            background: { ...SHARED_PROPERTIES.background, foundationFallback: 'primary' },
            foreground: { ...SHARED_PROPERTIES.foreground, foundationFallback: 'primaryForeground' },
            borderColor: { ...SHARED_PROPERTIES.borderColor, foundationFallback: 'border' },
            borderWidth: SHARED_PROPERTIES.borderWidth,
            borderRadius: { ...SHARED_PROPERTIES.borderRadius, foundationFallback: 'borderRadius' },
            fontSize: { ...SHARED_PROPERTIES.fontSize, foundationFallback: 'fontSize' },
            fontWeight: SHARED_PROPERTIES.fontWeight,
            padding: { ...SHARED_PROPERTIES.padding, foundationFallback: 'padding' },
        },
    },
    {
        id: 'nav-item',
        label: 'Nav item',
        category: 'layout',
        parts: [
            {
                id: 'default',
                label: 'Default',
                properties: NAV_ITEM_DEFAULT_PROPERTIES,
            },
            {
                id: 'selected',
                label: 'Selected',
                properties: NAV_ITEM_SELECTED_PROPERTIES,
            },
        ],
        properties: {},
    },
    {
        id: 'header',
        label: 'Header',
        category: 'layout',
        properties: {
            background: { ...SHARED_PROPERTIES.background, foundationFallback: 'background' },
            borderColor: { ...SHARED_PROPERTIES.borderColor, foundationFallback: 'border' },
            height: { ...SHARED_PROPERTIES.height, foundationFallback: 'headerHeight' },
            logoSize: {
                type: 'size',
                label: 'Logo size',
                sizePresets: ['xs', 'sm', 'md', 'lg', 'xl'],
                allowCustomValue: true,
            },
        },
    },
    {
        id: 'card',
        label: 'Card',
        category: 'component',
        properties: {
            background: { ...SHARED_PROPERTIES.background, foundationFallback: 'surface' },
            borderColor: { ...SHARED_PROPERTIES.borderColor, foundationFallback: 'border' },
            borderRadius: { ...SHARED_PROPERTIES.borderRadius, foundationFallback: 'borderRadius' },
            padding: { ...SHARED_PROPERTIES.padding, foundationFallback: 'padding' },
        },
    },
];

export function getElementDef(elementId: string): ElementDef | undefined {
    return POC_ELEMENT_REGISTRY.find(e => e.id === elementId);
}

export function resolvePartStorageKey(partId: string): string {
    return partId;
}

export function resolvePartCssVariant(partId: string): string | undefined {
    return partId === 'default' ? undefined : partId;
}
