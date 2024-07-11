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

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltip } from '@angular/material/tooltip';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { GioCardEmptyStateModule } from '@gravitee/ui-particles-angular';
import { MatExpansionModule } from '@angular/material/expansion';
import { RouterLink } from '@angular/router';

import { ApiScoreComponent } from './api-score.component';
import { ApiScoreListComponent } from './api-score-list/api-score-list.component';

import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';
import { GioTableWrapperModule } from '../../../shared/components/gio-table-wrapper/gio-table-wrapper.module';
import { MapProviderNamePipe } from '../../integrations/pipes/map-provider-name.pipe';

@NgModule({
  declarations: [ApiScoreComponent, ApiScoreListComponent],
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatExpansionModule,
    MatIconModule,
    MatTableModule,
    MatTooltip,
    GioPermissionModule,
    GioTableWrapperModule,
    MapProviderNamePipe,
    GioCardEmptyStateModule,
  ],
  exports: [ApiScoreComponent],
})
export class ApiScoreModule {}
