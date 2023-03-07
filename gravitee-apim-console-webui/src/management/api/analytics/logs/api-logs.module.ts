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
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { GioBannerModule, GioIconsModule, GioSaveBarModule, GioFormSlideToggleModule } from '@gravitee/ui-particles-angular';
import { MatDividerModule } from '@angular/material/divider';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBarModule } from '@angular/material/snack-bar';

import { ApiLogsConfigurationComponent } from './configuration/api-logs-configuration.component';

import { GioFormCardGroupModule } from '../../../../shared/components/gio-form-card-group/gio-form-card-group.module';

@NgModule({
  declarations: [ApiLogsConfigurationComponent],
  exports: [ApiLogsConfigurationComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatDividerModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    MatSnackBarModule,
    GioFormCardGroupModule,
    GioIconsModule,
    GioBannerModule,
    GioFormSlideToggleModule,
    GioSaveBarModule,
  ],
})
export class ApiLogsModule {}
