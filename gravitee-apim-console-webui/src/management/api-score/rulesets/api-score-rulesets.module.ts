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
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { AsyncPipe, NgIf } from '@angular/common';
import { MatMenuModule } from '@angular/material/menu';
import { MatIcon } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { MatError, MatFormField, MatHint, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { ReactiveFormsModule } from '@angular/forms';
import {
  GioBannerModule,
  GioCardEmptyStateModule,
  GioFormFilePickerModule,
  GioFormSelectionInlineModule,
  GioLoaderModule,
  GioSaveBarModule,
} from '@gravitee/ui-particles-angular';
import { MatTooltip } from '@angular/material/tooltip';

import { ApiScoreRulesetsComponent } from './api-score-rulesets.component';
import { ImportApiScoreRulesetComponent } from './import/import-api-score-ruleset.component';
import { EditApiScoreRulesetComponent } from './edit/edit-api-score-ruleset.component';

import { ApiImportFilePickerComponent } from '../../api/component/api-import-file-picker/api-import-file-picker.component';
import { GioPermissionModule } from '../../../shared/components/gio-permission/gio-permission.module';

@NgModule({
  declarations: [ApiScoreRulesetsComponent, ImportApiScoreRulesetComponent, EditApiScoreRulesetComponent],
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatMenuModule,
    MatExpansionModule,
    AsyncPipe,
    MatIcon,
    RouterLink,
    MatError,
    MatFormField,
    MatHint,
    MatInput,
    MatLabel,
    NgIf,
    GioFormFilePickerModule,
    GioFormSelectionInlineModule,
    GioCardEmptyStateModule,
    ApiImportFilePickerComponent,
    GioBannerModule,
    GioSaveBarModule,
    GioLoaderModule,
    GioPermissionModule,
    MatTooltip,
  ],
  exports: [ApiScoreRulesetsComponent, ImportApiScoreRulesetComponent, EditApiScoreRulesetComponent],
})
export class ApiScoreRulesetsModule {}
