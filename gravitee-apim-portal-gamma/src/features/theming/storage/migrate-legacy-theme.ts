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
import type {
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
        return {
            ...doc,
            instanceOverrides: doc.instanceOverrides ?? {},
        };
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
