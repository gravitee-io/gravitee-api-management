/*
 * Copyright (C) 2023 The Gravitee team (http://gravitee.io)
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
import { InjectionToken } from '@angular/core';

import { GraviteeMonacoEditorTheme } from './gravitee-monaco-editor-theme';

export type GraviteeMonacoEditorConfig = {
  baseUrl?: string;
  theme?: GraviteeMonacoEditorTheme;
};

export const GRAVITEE_MONACO_EDITOR_CONFIG = new InjectionToken<GraviteeMonacoEditorConfig>('gravitee-monaco-editor-config');
