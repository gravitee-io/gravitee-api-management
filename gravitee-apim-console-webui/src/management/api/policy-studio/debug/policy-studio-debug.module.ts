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
import { CommonModule, TitleCasePipe } from '@angular/common';
import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTreeModule } from '@angular/material/tree';
import { GioIconsModule, GioFormHeadersModule } from '@gravitee/ui-particles-angular';
import { MatTooltipModule } from '@angular/material/tooltip';

import { PolicyStudioDebugComponent } from './policy-studio-debug.component';
import { PolicyStudioDebugInspectorBodyComponent } from './components/policy-studio-debug-inspector/policy-studio-debug-inspector-body/policy-studio-debug-inspector-body.component';
import { PolicyStudioDebugInspectorComponent } from './components/policy-studio-debug-inspector/policy-studio-debug-inspector.component';
import { PolicyStudioDebugInspectorErrorComponent } from './components/policy-studio-debug-inspector/policy-studio-debug-inspector-error/policy-studio-debug-inspector-error.component';
import { PolicyStudioDebugInspectorTableComponent } from './components/policy-studio-debug-inspector/policy-studio-debug-inspector-table/policy-studio-debug-inspector-table.component';
import { PolicyStudioDebugRequestComponent } from './components/policy-studio-debug-request/policy-studio-debug-request.component';
import { PolicyStudioDebugResponseComponent } from './components/policy-studio-debug-response/policy-studio-debug-response.component';
import { PolicyStudioDebugTimelineCardComponent } from './components/policy-studio-debug-timeline-card/policy-studio-debug-timeline-card.component';
import { PolicyStudioDebugTimelineComponent } from './components/policy-studio-debug-timeline/policy-studio-debug-timeline.component';
import { PolicyStudioDebugTimelineLegendComponent } from './components/policy-studio-debug-timeline-legend/policy-studio-debug-timeline-legend.component';
import { PolicyStudioDebugInspectorTextComponent } from './components/policy-studio-debug-inspector/policy-studio-debug-inspector-text/policy-studio-debug-inspector-text.component';
import { PolicyStudioDebugTimelineOverviewComponent } from './components/policy-studio-debug-timeline-overview/policy-studio-debug-timeline-overview.component';
import { PolicyStudioDebugTimelineHoverComponent } from './components/policy-studio-debug-timeline-hover/policy-studio-debug-timeline-hover.directive';

import { GioDiffModule } from '../../../../shared/components/gio-diff/gio-diff.module';

@NgModule({
  imports: [
    CommonModule,
    ReactiveFormsModule,

    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatSelectModule,
    MatSnackBarModule,
    MatTabsModule,
    MatTreeModule,
    MatTooltipModule,

    GioIconsModule,
    GioDiffModule,
    GioFormHeadersModule,
  ],
  declarations: [
    PolicyStudioDebugComponent,
    PolicyStudioDebugInspectorComponent,
    PolicyStudioDebugInspectorBodyComponent,
    PolicyStudioDebugInspectorErrorComponent,
    PolicyStudioDebugInspectorTableComponent,
    PolicyStudioDebugInspectorTextComponent,
    PolicyStudioDebugRequestComponent,
    PolicyStudioDebugResponseComponent,
    PolicyStudioDebugTimelineCardComponent,
    PolicyStudioDebugTimelineComponent,
    PolicyStudioDebugTimelineOverviewComponent,
    PolicyStudioDebugTimelineLegendComponent,
    PolicyStudioDebugTimelineHoverComponent,
  ],
  exports: [PolicyStudioDebugComponent],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  providers: [TitleCasePipe],
})
export class PolicyStudioDebugModule {}
