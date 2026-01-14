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
import { HookParserEntry } from 'ngx-dynamic-hooks';

import { GmdMdComponent } from './block/gmd-md.component';
import { CellComponent } from './grid/cell/cell.component';
import { GridComponent } from './grid/grid.component';
import { ComponentSelector } from '../models/componentSelector';
import { GmdButtonComponent } from './button/gmd-button.component';
import { GmdCardSubtitleComponent } from './card/components/card-subtitle/gmd-card-subtitle.component';
import { GmdCardTitleComponent } from './card/components/card-title/gmd-card-title.component';
import { GmdCardComponent } from './card/gmd-card.component';
import { GmdCheckboxComponent } from './checkbox/gmd-checkbox.component';
import { GmdInputComponent } from './input/gmd-input.component';
import { GmdRadioComponent } from './radio/gmd-radio.component';
import { GmdSelectComponent } from './select/gmd-select.component';
import { GmdTextareaComponent } from './textarea/gmd-textarea.component';

export const prefixStripperParser: HookParserEntry[] = [
  {
    component: GridComponent,
    selector: ComponentSelector.GRID,
  },
  {
    component: CellComponent,
    selector: ComponentSelector.CELL,
  },
  {
    component: GmdCardComponent,
    selector: ComponentSelector.CARD,
  },
  {
    component: GmdCardTitleComponent,
    selector: ComponentSelector.CARD_TITLE,
  },
  {
    component: GmdCardSubtitleComponent,
    selector: ComponentSelector.CARD_SUBTITLE,
  },
  {
    component: GmdMdComponent,
    selector: ComponentSelector.MD_BLOCK,
  },
  {
    component: GmdButtonComponent,
    selector: ComponentSelector.BUTTON,
  },
  {
    component: GmdInputComponent,
    selector: ComponentSelector.INPUT,
  },
  {
    component: GmdTextareaComponent,
    selector: ComponentSelector.TEXTAREA,
  },
  {
    component: GmdSelectComponent,
    selector: ComponentSelector.SELECT,
  },
  {
    component: GmdCheckboxComponent,
    selector: ComponentSelector.CHECKBOX,
  },
  {
    component: GmdRadioComponent,
    selector: ComponentSelector.RADIO,
  },
];
