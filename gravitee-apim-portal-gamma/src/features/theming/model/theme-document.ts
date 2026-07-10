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

export type ThemeColorMode = 'light' | 'dark' | 'system';

export interface FoundationTokens {
    readonly primary: string;
    readonly primaryForeground: string;
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
    readonly fontFamily: string;
    readonly headingFontFamily: string;
    readonly fontSize: string;
    readonly lineHeight: string;
    readonly borderRadius: string;
    readonly borderWidth: string;
    readonly padding: string;
    readonly maxWidth: string;
    readonly sidebarWidth: string;
    readonly headerHeight: string;
    readonly footerHeight: string;
}

export interface CustomVariableDefinition {
    readonly name: string;
    readonly lightValue: string;
    readonly darkValue: string;
}

export interface ElementModeTokens {
    readonly light: Record<string, string>;
    readonly dark: Record<string, string>;
}

export type ElementTokens =
    | ElementModeTokens
    | Record<string, ElementModeTokens>;

export interface PortalThemeDocument {
    readonly schemaVersion: 1;
    readonly id: string;
    readonly portalId: string;
    readonly activeMode: ThemeColorMode;
    readonly foundation: {
        readonly light: Partial<FoundationTokens>;
        readonly dark: Partial<FoundationTokens>;
    };
    readonly elements: Record<string, ElementTokens>;
    readonly customVariables: readonly CustomVariableDefinition[];
    readonly instanceOverrides: Record<string, Record<string, string>>;
}

/** @deprecated Legacy shape stored in IndexedDB before schemaVersion 1 */
export interface LegacyPortalTheme {
    readonly id: string;
    readonly portalId: string;
    readonly tokens: {
        readonly light: LegacyThemeTokens;
        readonly dark: LegacyThemeTokens;
    };
    readonly customVariables: readonly LegacyCustomVariable[];
    readonly activeMode: ThemeColorMode;
}

export interface LegacyThemeTokens {
    readonly colors: Record<string, string>;
    readonly typography: Record<string, string | number>;
    readonly spacing: Record<string, string>;
    readonly layout: Record<string, string>;
}

export interface LegacyCustomVariable {
    readonly id: string;
    readonly name: string;
    readonly lightValue: string;
    readonly darkValue: string;
}
