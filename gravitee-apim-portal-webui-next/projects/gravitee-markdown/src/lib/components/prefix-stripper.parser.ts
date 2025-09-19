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

import { CellComponent } from './grid/cell/cell.component';
import { GridComponent } from './grid/grid.component';
import { ComponentSelector } from '../models/componentSelector';
import { GraviteeMarkdownCardSubtitleComponent } from './card/components/card-subtitle/gravitee-markdown-card-subtitle.component';
import { GraviteeMarkdownCardTitleComponent } from './card/components/card-title/gravitee-markdown-card-title.component';
import { GraviteeMarkdownCardComponent } from './card/gravitee-markdown-card.component';

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
    component: GraviteeMarkdownCardComponent,
    selector: ComponentSelector.CARD,
  },
  {
    component: GraviteeMarkdownCardTitleComponent,
    selector: ComponentSelector.CARD_TITLE,
  },
  {
    component: GraviteeMarkdownCardSubtitleComponent,
    selector: ComponentSelector.CARD_SUBTITLE,
  },
];
