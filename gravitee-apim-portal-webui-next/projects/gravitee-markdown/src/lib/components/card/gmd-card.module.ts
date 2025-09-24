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
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { GmdCardTitleComponent } from './components/card-title/gmd-card-title.component';
import { GmdCardComponent } from './gmd-card.component';
import { GmdMdComponent } from '../block/gmd-md.component';
import { GmdCardSubtitleComponent } from './components/card-subtitle/gmd-card-subtitle.component';

@NgModule({
  imports: [CommonModule, GmdMdComponent, GmdCardTitleComponent, GmdCardSubtitleComponent],
  declarations: [GmdCardComponent],
  exports: [GmdCardComponent, GmdCardTitleComponent, GmdCardSubtitleComponent],
})
export class GmdCardModule {}
