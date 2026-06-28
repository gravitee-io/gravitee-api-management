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
import type { ThemeTokens, ThemeMode } from '../types';

export const DEFAULT_LIGHT_TOKENS: ThemeTokens = {
    colors: {
        primary: '#6366f1',
        secondary: '#8b5cf6',
        background: '#ffffff',
        surface: '#f8fafc',
        text: '#0f172a',
        muted: '#f1f5f9',
        mutedForeground: '#64748b',
        accent: '#6366f1',
        border: '#e2e8f0',
        ring: '#6366f1',
        destructive: '#ef4444',
        link: '#6366f1',
    },
    typography: {
        fontFamily: 'Inter, system-ui, -apple-system, sans-serif',
        headingFontFamily: 'Inter, system-ui, -apple-system, sans-serif',
        fontSize: '14px',
        lineHeight: '1.6',
        headingScale: 1.25,
    },
    spacing: {
        borderRadius: '6px',
        borderWidth: '1px',
        padding: '16px',
    },
    layout: {
        maxWidth: '1200px',
        sidebarWidth: '240px',
        headerHeight: '48px',
        footerHeight: '40px',
    },
};

export const DEFAULT_DARK_TOKENS: ThemeTokens = {
    colors: {
        primary: '#818cf8',
        secondary: '#a78bfa',
        background: '#0f172a',
        surface: '#1e293b',
        text: '#f1f5f9',
        muted: '#1e293b',
        mutedForeground: '#94a3b8',
        accent: '#818cf8',
        border: '#334155',
        ring: '#818cf8',
        destructive: '#f87171',
        link: '#818cf8',
    },
    typography: { ...DEFAULT_LIGHT_TOKENS.typography },
    spacing: { ...DEFAULT_LIGHT_TOKENS.spacing },
    layout: { ...DEFAULT_LIGHT_TOKENS.layout },
};

export const DEFAULT_THEME_MODE: ThemeMode = {
    light: DEFAULT_LIGHT_TOKENS,
    dark: DEFAULT_DARK_TOKENS,
};

export function createDefaultTheme(portalId: string, id?: string): {
    id: string;
    portalId: string;
    tokens: ThemeMode;
    customVariables: never[];
    activeMode: 'system';
} {
    return {
        id: id ?? `theme-${portalId}`,
        portalId,
        tokens: DEFAULT_THEME_MODE,
        customVariables: [],
        activeMode: 'system',
    };
}
