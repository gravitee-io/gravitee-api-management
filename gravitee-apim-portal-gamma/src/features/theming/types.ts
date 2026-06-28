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

export interface TypographyTokens {
    readonly fontFamily: string;
    readonly headingFontFamily: string;
    readonly fontSize: string;
    readonly lineHeight: string;
    readonly headingScale: number;
}

export interface SpacingTokens {
    readonly borderRadius: string;
    readonly borderWidth: string;
    readonly padding: string;
}

export interface LayoutTokens {
    readonly maxWidth: string;
    readonly sidebarWidth: string;
    readonly headerHeight: string;
    readonly footerHeight: string;
}

export interface ThemeTokens {
    readonly colors: ColorTokens;
    readonly typography: TypographyTokens;
    readonly spacing: SpacingTokens;
    readonly layout: LayoutTokens;
}

export interface ThemeMode {
    readonly light: ThemeTokens;
    readonly dark: ThemeTokens;
}

export interface CustomVariable {
    readonly id: string;
    readonly name: string;
    readonly lightValue: string;
    readonly darkValue: string;
}

export type ThemeColorMode = 'light' | 'dark' | 'system';

export interface PortalTheme {
    readonly id: string;
    readonly portalId: string;
    readonly tokens: ThemeMode;
    readonly customVariables: readonly CustomVariable[];
    readonly activeMode: ThemeColorMode;
}

export type ThemeTokenCategory = keyof ThemeTokens;
export type ThemeTokenKey<C extends ThemeTokenCategory> = keyof ThemeTokens[C];
