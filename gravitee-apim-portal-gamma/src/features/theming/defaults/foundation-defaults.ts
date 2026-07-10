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
import type { FoundationTokens } from '../model/theme-document';

export const DEFAULT_LIGHT_FOUNDATION: FoundationTokens = {
    primary: '#6366f1',
    primaryForeground: '#ffffff',
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
    fontFamily: 'Inter, system-ui, -apple-system, sans-serif',
    headingFontFamily: 'Inter, system-ui, -apple-system, sans-serif',
    fontSize: '14px',
    lineHeight: '1.6',
    borderRadius: '6px',
    borderWidth: '1px',
    padding: '16px',
    maxWidth: '1200px',
    sidebarWidth: '240px',
    headerHeight: '48px',
    footerHeight: '40px',
};

export const DEFAULT_DARK_FOUNDATION: FoundationTokens = {
    primary: '#818cf8',
    primaryForeground: '#0f172a',
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
    fontFamily: DEFAULT_LIGHT_FOUNDATION.fontFamily,
    headingFontFamily: DEFAULT_LIGHT_FOUNDATION.headingFontFamily,
    fontSize: DEFAULT_LIGHT_FOUNDATION.fontSize,
    lineHeight: DEFAULT_LIGHT_FOUNDATION.lineHeight,
    borderRadius: DEFAULT_LIGHT_FOUNDATION.borderRadius,
    borderWidth: DEFAULT_LIGHT_FOUNDATION.borderWidth,
    padding: DEFAULT_LIGHT_FOUNDATION.padding,
    maxWidth: DEFAULT_LIGHT_FOUNDATION.maxWidth,
    sidebarWidth: DEFAULT_LIGHT_FOUNDATION.sidebarWidth,
    headerHeight: DEFAULT_LIGHT_FOUNDATION.headerHeight,
    footerHeight: DEFAULT_LIGHT_FOUNDATION.footerHeight,
};

export function resolveFoundation(
    overrides: Partial<FoundationTokens> | undefined,
    mode: 'light' | 'dark',
): FoundationTokens {
    const defaults = mode === 'light' ? DEFAULT_LIGHT_FOUNDATION : DEFAULT_DARK_FOUNDATION;
    return { ...defaults, ...overrides };
}

/** Colors and typography are always emitted so the shell and Graphene bridge have baselines. */
export const FOUNDATION_BASELINE_KEYS = new Set<keyof FoundationTokens>([
    'primary', 'primaryForeground', 'secondary', 'background', 'surface', 'text',
    'muted', 'mutedForeground', 'accent', 'border', 'ring', 'destructive', 'link',
    'fontFamily', 'headingFontFamily', 'fontSize', 'lineHeight',
]);

/** Spacing and layout tokens only apply when explicitly overridden in the theme document. */
export const FOUNDATION_OPTIONAL_KEYS = new Set<keyof FoundationTokens>([
    'borderRadius', 'borderWidth', 'padding', 'maxWidth', 'sidebarWidth', 'headerHeight', 'footerHeight',
]);
