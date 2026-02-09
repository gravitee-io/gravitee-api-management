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

import { GmdButtonComponent } from './button/gmd-button.component';
import { GmdCardComponent } from './card/gmd-card.component';
import { GmdCheckboxComponent } from './checkbox/gmd-checkbox.component';
import { GridComponent } from './grid/grid.component';
import { GmdInputComponent } from './input/gmd-input.component';
import { GmdRadioComponent } from './radio/gmd-radio.component';
import { GmdSelectComponent } from './select/gmd-select.component';
import { GmdTextareaComponent } from './textarea/gmd-textarea.component';

interface InputMetadata {
  propName: string;
  templateName: string;
}

function getComponentInputNames(componentType: Type<unknown>): string[] {
  const mirror = reflectComponentType(componentType);

  if (!mirror) {
    return [];
  }

  const inputMetadata: InputMetadata[] = [...mirror.inputs];

  // Map the metadata to return only the template-facing name (the name used in the HTML tag)
  return inputMetadata.map(input => input.templateName);
}

export const componentAttributeNames = [
  ...getComponentInputNames(GmdCardComponent),
  ...getComponentInputNames(GridComponent),
  ...getComponentInputNames(GmdButtonComponent),
  ...getComponentInputNames(GmdInputComponent),
  ...getComponentInputNames(GmdTextareaComponent),
  ...getComponentInputNames(GmdSelectComponent),
  ...getComponentInputNames(GmdCheckboxComponent),
  ...getComponentInputNames(GmdRadioComponent),
];
