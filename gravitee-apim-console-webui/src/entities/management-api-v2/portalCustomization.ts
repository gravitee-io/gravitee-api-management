/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
export type ThemeType = 'PORTAL' | 'PORTAL_NEXT';

interface GenericTheme {
  id: string;
  name: string;
  type: ThemeType;
  createdAt: Date;
  updatedAt: Date;
  enabled: boolean;
  logo: string;
  optionalLogo?: string;
  favicon: string;
}

export type Theme = ThemePortal | ThemePortalNext;

export interface ThemePortal extends GenericTheme {
  definition: PortalDefinition[];
  backgroundImage: string;
}

export interface PortalDefinition {
  name: string;
  css: PortalCssDefinition[];
}

export interface PortalCssDefinition {
  name: string;
  description: string;
  value: string;
  defaultValue: string;
  type: 'color' | 'length' | 'string' | 'image';
}

export interface PortalNextDefinition {
  color: PortalNextDefinitionColor;
  font: PortalNextDefinitionFont;
  customCss?: string;
}

export interface PortalNextDefinitionColor {
  primary: string;
  secondary: string;
  tertiary: string;
  error: string;
  pageBackground: string;
  cardBackground: string;
}

export interface PortalNextDefinitionFont {
  fontFamily: string;
}

export interface ThemePortalNext extends GenericTheme {
  definition: PortalNextDefinition;
}

export type GenericUpdateTheme = Omit<GenericTheme, 'createdAt' | 'updatedAt'>;

export type UpdateTheme = UpdateThemePortal | UpdateThemePortalNext;

export interface UpdateThemePortal extends GenericUpdateTheme {
  definition: PortalDefinition[];
  backgroundImage: string;
}

export interface UpdateThemePortalNext extends GenericUpdateTheme {
  definition: PortalNextDefinition;
}
