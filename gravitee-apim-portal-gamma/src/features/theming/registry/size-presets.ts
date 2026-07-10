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

export type SizePreset = 'xs' | 'sm' | 'md' | 'lg' | 'xl';

const BORDER_RADIUS_PRESETS: Record<SizePreset, string> = {
    xs: '2px',
    sm: '4px',
    md: '6px',
    lg: '8px',
    xl: '12px',
};

const FONT_SIZE_PRESETS: Record<SizePreset, string> = {
    xs: '0.75rem',
    sm: '0.875rem',
    md: '1rem',
    lg: '1.125rem',
    xl: '1.25rem',
};

const PADDING_PRESETS: Record<SizePreset, string> = {
    xs: '0.25rem',
    sm: '0.5rem',
    md: '1rem',
    lg: '1.5rem',
    xl: '2rem',
};

const BORDER_WIDTH_PRESETS: Record<SizePreset, string> = {
    xs: '1px',
    sm: '1px',
    md: '2px',
    lg: '3px',
    xl: '4px',
};

const HEIGHT_PRESETS: Record<SizePreset, string> = {
    xs: '32px',
    sm: '40px',
    md: '48px',
    lg: '56px',
    xl: '64px',
};

const PRESET_MAPS: Record<string, Record<SizePreset, string>> = {
    borderRadius: BORDER_RADIUS_PRESETS,
    fontSize: FONT_SIZE_PRESETS,
    padding: PADDING_PRESETS,
    borderWidth: BORDER_WIDTH_PRESETS,
    height: HEIGHT_PRESETS,
};

const ALL_PRESETS = new Set<string>(['xs', 'sm', 'md', 'lg', 'xl']);

export function isSizePreset(value: string): value is SizePreset {
    return ALL_PRESETS.has(value);
}

export function resolveSizeValue(property: string, value: string): string {
    const presetMap = PRESET_MAPS[property];
    if (presetMap && isSizePreset(value)) {
        return presetMap[value];
    }
    return value;
}
