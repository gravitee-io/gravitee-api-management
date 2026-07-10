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

export type {
    CustomVariableDefinition,
    ElementModeTokens,
    ElementTokens,
    FoundationTokens,
    LegacyCustomVariable,
    LegacyPortalTheme,
    LegacyThemeTokens,
    PortalThemeDocument,
    ThemeColorMode,
} from './model/theme-document';

/** Alias for PortalThemeDocument — the active theme model */
export type PortalTheme = import('./model/theme-document').PortalThemeDocument;

export type CustomVariable = import('./model/theme-document').CustomVariableDefinition;

/** @deprecated Legacy nested token shape — use FoundationTokens */
export interface ColorTokens {
    readonly primary: string;
    readonly secondary: string;
    readonly background: string;
    readonly surface: string;
    readonly text: string;
    readonly muted: string;
    readonly mutedForeground: string;
    readonly accent: string;
    readonly border: string;
    readonly ring: string;
    readonly destructive: string;
    readonly link: string;
}

/** @deprecated Legacy nested token shape */
export interface TypographyTokens {
    readonly fontFamily: string;
    readonly headingFontFamily: string;
    readonly fontSize: string;
    readonly lineHeight: string;
    readonly headingScale: number;
}

/** @deprecated Legacy nested token shape */
export interface SpacingTokens {
    readonly borderRadius: string;
    readonly borderWidth: string;
    readonly padding: string;
}

/** @deprecated Legacy nested token shape */
export interface LayoutTokens {
    readonly maxWidth: string;
    readonly sidebarWidth: string;
    readonly headerHeight: string;
    readonly footerHeight: string;
}

/** @deprecated Legacy nested token shape */
export interface ThemeTokens {
    readonly colors: ColorTokens;
    readonly typography: TypographyTokens;
    readonly spacing: SpacingTokens;
    readonly layout: LayoutTokens;
}

/** @deprecated Legacy nested token shape */
export interface ThemeMode {
    readonly light: ThemeTokens;
    readonly dark: ThemeTokens;
}

export type ThemeTokenCategory = 'colors' | 'typography' | 'spacing' | 'layout';

export type FoundationTokenKey = keyof import('./model/theme-document').FoundationTokens;
