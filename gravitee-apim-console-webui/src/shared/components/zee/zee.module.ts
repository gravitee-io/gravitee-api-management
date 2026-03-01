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
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgModule } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';

import { ZeeWidgetComponent } from './zee-widget/zee-widget.component';
import { ZeePreviewComponent } from './zee-preview/zee-preview.component';
import { FlowCardComponent } from './zee-structured-view/flow-card.component';
import { PlanCardComponent } from './zee-structured-view/plan-card.component';
import { EndpointCardComponent } from './zee-structured-view/endpoint-card.component';
import { EntrypointCardComponent } from './zee-structured-view/entrypoint-card.component';
import { ApiCardComponent } from './zee-structured-view/api-card.component';

@NgModule({
  declarations: [ZeeWidgetComponent, ZeePreviewComponent, FlowCardComponent, PlanCardComponent, EndpointCardComponent, EntrypointCardComponent, ApiCardComponent],
  exports: [ZeeWidgetComponent, ZeePreviewComponent, FlowCardComponent, PlanCardComponent, EndpointCardComponent, EntrypointCardComponent, ApiCardComponent],
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatListModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTabsModule,
    MatTooltipModule,
  ],
})
export class ZeeModule {}
