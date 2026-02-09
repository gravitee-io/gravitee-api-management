/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { reflectComponentType, Type } from '@angular/core';

import { GmdCheckboxComponent } from './checkbox/gmd-checkbox.component';
import { GmdInputComponent } from './input/gmd-input.component';
import { GmdRadioComponent } from './radio/gmd-radio.component';
import { GmdSelectComponent } from './select/gmd-select.component';
import { GmdTextareaComponent } from './textarea/gmd-textarea.component';

/**
 * Extract the component selector from an Angular component type.
 * @param componentType The Angular component class
 * @returns The selector string (e.g., 'gmd-input') or null if not found
 */
function getComponentSelector(componentType: Type<unknown>): string | null {
  const mirror = reflectComponentType(componentType);
  return mirror?.selector ?? null;
}

/**
 * Extract the component name without 'gmd-' prefix from an Angular component type.
 * @param componentType The Angular component class
 * @returns The component name without prefix (e.g., 'input') or empty string if not found
 */
function getComponentName(componentType: Type<unknown>): string {
  const selector = getComponentSelector(componentType);
  return selector ? selector.replace('gmd-', '') : '';
}

/**
 * List of component names (without 'gmd-' prefix) that should be normalized from self-closing tags.
 * Dynamically extracted from component selectors using Angular reflection.
 * Used for normalizing self-closing tags in the markdown renderer (e.g., <gmd-input /> -> <gmd-input></gmd-input>).
 */
export const selfClosingComponentNames = [
  getComponentName(GmdInputComponent),
  getComponentName(GmdTextareaComponent),
  getComponentName(GmdSelectComponent),
  getComponentName(GmdCheckboxComponent),
  getComponentName(GmdRadioComponent),
];
