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
export enum ComponentSelector {
  GRID = 'gmd-grid',
  CELL = 'gmd-cell',
  CARD = 'gmd-card',
  CARD_TITLE = 'gmd-card-title',
  CARD_SUBTITLE = 'gmd-card-subtitle',
  MD_BLOCK = 'gmd-md',
  BUTTON = 'gmd-button',
  INPUT = 'gmd-input',
  TEXTAREA = 'gmd-textarea',
  SELECT = 'gmd-select',
  CHECKBOX = 'gmd-checkbox',
  RADIO = 'gmd-radio',
}

export function getComponentSelector(componentTag: string): ComponentSelector | undefined {
  return Object.values(ComponentSelector).find((selector): selector is ComponentSelector => selector === componentTag) as
    | ComponentSelector
    | undefined;
}
