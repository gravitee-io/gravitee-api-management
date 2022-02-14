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
import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { GioIconsModule } from '@gravitee/ui-particles-angular';

import { PolicyStudioDebugRequestComponent } from './components/policy-studio-debug-request/policy-studio-debug-request.component';
import { PolicyStudioDebugResponseTryItComponent } from './components/policy-studio-debug-response-try-it/policy-studio-debug-response-try-it.component';
import { PolicyStudioDebugResponseComponent } from './components/policy-studio-debug-response/policy-studio-debug-response.component';
import { PolicyStudioDebugTimelineCardComponent } from './components/policy-studio-debug-timeline-card/policy-studio-debug-timeline-card.component';
import { PolicyStudioDebugTimelineComponent } from './components/policy-studio-debug-timeline/policy-studio-debug-timeline.component';
import { PolicyStudioDebugComponent } from './policy-studio-debug.component';
import { PolicyStudioDebugTimelineLegendComponent } from './components/policy-studio-debug-timeline-legend/policy-studio-debug-timeline-legend.component';

@NgModule({
  imports: [
    CommonModule,
    ReactiveFormsModule,

    MatFormFieldModule,
    MatInputModule,
    MatTabsModule,
    MatSnackBarModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,

    GioIconsModule,
  ],
  declarations: [
    PolicyStudioDebugComponent,
    PolicyStudioDebugRequestComponent,
    PolicyStudioDebugResponseComponent,
    PolicyStudioDebugResponseTryItComponent,
    PolicyStudioDebugTimelineCardComponent,
    PolicyStudioDebugTimelineComponent,
    PolicyStudioDebugTimelineLegendComponent,
  ],
  exports: [PolicyStudioDebugComponent],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class PolicyStudioDebugModule {}
