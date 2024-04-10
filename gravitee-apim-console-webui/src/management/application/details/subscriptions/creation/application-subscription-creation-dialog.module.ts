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
import { MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { GioAvatarModule, GioFormJsonSchemaModule, GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatInputModule } from '@angular/material/input';
import { MatRadioButton, MatRadioGroup } from '@angular/material/radio';
import { MatSelect } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';

import { ApplicationSubscriptionCreationDialogComponent } from './application-subscription-creation-dialog.component';

@NgModule({
  declarations: [ApplicationSubscriptionCreationDialogComponent],
  exports: [ApplicationSubscriptionCreationDialogComponent],
  imports: [
    CommonModule,
    FormsModule,
    GioAvatarModule,
    GioIconsModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule,
    ReactiveFormsModule,
    MatRadioButton,
    MatRadioGroup,
    GioFormJsonSchemaModule,
    MatSelect,
  ],
})
export class ApplicationSubscriptionCreationDialogModule {}
