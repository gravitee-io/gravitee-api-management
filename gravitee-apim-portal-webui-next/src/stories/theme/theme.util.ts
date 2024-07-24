/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Args } from '@storybook/angular';

import { addPropertyToDocument, addHslToDocument } from '../../services/theme.service';

const CUSTOMIZATION_ARGS = {
  primary: {
    control: 'color',
  },
  secondary: {
    control: 'color',
  },
  tertiary: {
    control: 'color',
  },
  error: {
    control: 'color',
  },
  background: {
    control: 'color',
  },
  bannerBackground: {
    control: 'color',
  },
  bannerText: {
    control: 'color',
  },
};

export interface CustomizationConfig {
  primary?: string;
  secondary?: string;
  tertiary?: string;
  error?: string;
  background?: string;
  bannerBackground?: string;
  bannerText?: string;
}

const CSS_VAR = {
  primary: '--gio-app-primary-main-color',
  secondary: '--gio-app-secondary-main-color',
  tertiary: '--gio-app-tertiary-main-color',
  error: '--gio-app-error-main-color',
  background: '--gio-app-background-color',
  bannerBackground: '--gio-banner-background-color',
  bannerText: '--gio-banner-text-color',
};

/**
 * Mimics how styles are injected in theme.service.ts.
 * @param args
 */
const computeAndInjectThemeForStory = (args: Args): void => {
  if (!document) {
    return;
  }
  resetTheme();
  computeStyles({ ...args });
  computePalette({ ...args });
};

const computePalette = (config: CustomizationConfig) => {
  addHslToDocument(CSS_VAR.primary, '#613CB0', config.primary);
  addHslToDocument(CSS_VAR.secondary, '#958BA9', config.secondary);
  addHslToDocument(CSS_VAR.tertiary, '#B7818F', config.tertiary);
  addHslToDocument(CSS_VAR.error, '#EC6152', config.error);
};

const computeStyles = (theme: CustomizationConfig): void => {
  addPropertyToDocument(CSS_VAR.background, theme.background);
  addPropertyToDocument(CSS_VAR.bannerBackground, theme.bannerBackground);
  addPropertyToDocument(CSS_VAR.bannerText, theme.bannerText);
};

const resetTheme = (): void => {
  document.documentElement.style.removeProperty(CSS_VAR.background);
  document.documentElement.style.removeProperty(CSS_VAR.bannerBackground);
  document.documentElement.style.removeProperty(CSS_VAR.bannerText);
  computePalette({});
};

export { computeAndInjectThemeForStory, resetTheme, CUSTOMIZATION_ARGS };
