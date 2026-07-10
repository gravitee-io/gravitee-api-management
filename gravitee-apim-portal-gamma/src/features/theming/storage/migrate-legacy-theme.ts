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
import {
    DEFAULT_DARK_FOUNDATION,
    DEFAULT_LIGHT_FOUNDATION,
    FOUNDATION_OPTIONAL_KEYS,
} from '../defaults/foundation-defaults';
import type {
    ElementModeTokens,
    FoundationTokens,
    LegacyPortalTheme,
    PortalThemeDocument,
} from '../model/theme-document';

function mapLegacyColorsToFoundation(colors: Record<string, string>): Partial<FoundationTokens> {
    return {
        primary: colors.primary,
        secondary: colors.secondary,
        background: colors.background,
        surface: colors.surface,
        text: colors.text,
        muted: colors.muted,
        mutedForeground: colors.mutedForeground,
        accent: colors.accent,
        border: colors.border,
        ring: colors.ring,
        destructive: colors.destructive,
        link: colors.link,
        primaryForeground: colors.primaryForeground ?? '#ffffff',
    };
}

function mapLegacyTokensToFoundation(tokens: LegacyPortalTheme['tokens']['light']): Partial<FoundationTokens> {
    return {
        ...mapLegacyColorsToFoundation(tokens.colors),
        fontFamily: String(tokens.typography.fontFamily ?? ''),
        headingFontFamily: String(tokens.typography.headingFontFamily ?? ''),
        fontSize: String(tokens.typography.fontSize ?? ''),
        lineHeight: String(tokens.typography.lineHeight ?? ''),
        borderRadius: tokens.spacing.borderRadius,
        borderWidth: tokens.spacing.borderWidth,
        padding: tokens.spacing.padding,
        maxWidth: tokens.layout.maxWidth,
        sidebarWidth: tokens.layout.sidebarWidth,
        headerHeight: tokens.layout.headerHeight,
        footerHeight: tokens.layout.footerHeight,
    };
}

function deleteEmptyModeTokens(modeTokens: Record<string, string>): Record<string, string> {
    const next: Record<string, string> = {};
    for (const [key, value] of Object.entries(modeTokens)) {
        if (value !== '') {
            next[key] = value;
        }
    }
    return next;
}

function sanitizeElementModeTokens(tokens: ElementModeTokens): ElementModeTokens {
    return {
        light: deleteEmptyModeTokens(tokens.light ?? {}),
        dark: deleteEmptyModeTokens(tokens.dark ?? {}),
    };
}

function sanitizeElements(
    elements: PortalThemeDocument['elements'] | undefined,
): PortalThemeDocument['elements'] {
    const next: PortalThemeDocument['elements'] = {};
    for (const [elementId, entry] of Object.entries(elements ?? {})) {
        if ('light' in entry && 'dark' in entry) {
            next[elementId] = sanitizeElementModeTokens(entry as ElementModeTokens);
        } else {
            const variants: Record<string, ElementModeTokens> = {};
            for (const [variant, variantTokens] of Object.entries(entry as Record<string, ElementModeTokens>)) {
                variants[variant] = sanitizeElementModeTokens(variantTokens);
            }
            next[elementId] = variants;
        }
    }
    return next;
}

function stripDefaultOptionalFoundationTokens(
    modeTokens: Record<string, string>,
    mode: 'light' | 'dark',
): Record<string, string> {
    const defaults = mode === 'light' ? DEFAULT_LIGHT_FOUNDATION : DEFAULT_DARK_FOUNDATION;
    const next = { ...modeTokens };
    for (const key of FOUNDATION_OPTIONAL_KEYS) {
        if (next[key] === defaults[key]) {
            delete next[key];
        }
    }
    return next;
}

function sanitizeFoundationModeTokens(
    modeTokens: Record<string, string>,
    mode: 'light' | 'dark',
): Partial<FoundationTokens> {
    const withoutEmpty = deleteEmptyModeTokens(modeTokens);
    return stripDefaultOptionalFoundationTokens(withoutEmpty, mode) as Partial<FoundationTokens>;
}

function sanitizeFoundation(
    foundation: PortalThemeDocument['foundation'] | undefined,
): PortalThemeDocument['foundation'] {
    return {
        light: sanitizeFoundationModeTokens(foundation?.light ?? {}, 'light'),
        dark: sanitizeFoundationModeTokens(foundation?.dark ?? {}, 'dark'),
    };
}

export function sanitizeThemeDocument(doc: PortalThemeDocument): PortalThemeDocument {
    return {
        ...doc,
        foundation: sanitizeFoundation(doc.foundation),
        elements: sanitizeElements(doc.elements),
    };
}

export function isLegacyPortalTheme(value: unknown): value is LegacyPortalTheme {
    if (!value || typeof value !== 'object') {
        return false;
    }
    const record = value as Record<string, unknown>;
    return record.schemaVersion === undefined && record.tokens !== undefined;
}

export function migrateLegacyTheme(legacy: LegacyPortalTheme): PortalThemeDocument {
    return {
        schemaVersion: 1,
        id: legacy.id,
        portalId: legacy.portalId,
        activeMode: legacy.activeMode,
        foundation: {
            light: mapLegacyTokensToFoundation(legacy.tokens.light),
            dark: mapLegacyTokensToFoundation(legacy.tokens.dark),
        },
        elements: {},
        customVariables: legacy.customVariables.map(({ name, lightValue, darkValue }) => ({
            name,
            lightValue,
            darkValue,
        })),
        instanceOverrides: {},
    };
}

export function normalizeThemeDocument(stored: unknown, portalId: string): PortalThemeDocument {
    if (!stored || typeof stored !== 'object') {
        return createDefaultThemeDocument(portalId);
    }

    if (isLegacyPortalTheme(stored)) {
        return migrateLegacyTheme(stored);
    }

    const doc = stored as PortalThemeDocument;
    if (doc.schemaVersion === 1) {
        return sanitizeThemeDocument({
            ...doc,
            instanceOverrides: doc.instanceOverrides ?? {},
        });
    }

    return createDefaultThemeDocument(portalId);
}

export function createDefaultThemeDocument(portalId: string, id?: string): PortalThemeDocument {
    return {
        schemaVersion: 1,
        id: id ?? `theme-${portalId}`,
        portalId,
        activeMode: 'system',
        foundation: { light: {}, dark: {} },
        elements: {},
        customVariables: [],
        instanceOverrides: {},
    };
}
