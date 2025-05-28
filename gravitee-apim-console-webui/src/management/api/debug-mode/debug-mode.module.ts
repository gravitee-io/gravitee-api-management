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
import { GioClipboardModule, GioFormHeadersModule, GioIconsModule, GioMonacoEditorModule } from '@gravitee/ui-particles-angular';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatCardModule } from '@angular/material/card';

import { DebugModeComponent } from './debug-mode.component';
import { DebugModeInspectorBodyComponent } from './components/debug-mode-inspector/debug-mode-inspector-body/debug-mode-inspector-body.component';
import { DebugModeInspectorComponent } from './components/debug-mode-inspector/debug-mode-inspector.component';
import { DebugModeInspectorErrorComponent } from './components/debug-mode-inspector/debug-mode-inspector-error/debug-mode-inspector-error.component';
import { DebugModeInspectorTableComponent } from './components/debug-mode-inspector/debug-mode-inspector-table/debug-mode-inspector-table.component';
import { DebugModeRequestComponent } from './components/debug-mode-request/debug-mode-request.component';
import { DebugModeResponseComponent } from './components/debug-mode-response/debug-mode-response.component';
import { DebugModeTimelineCardComponent } from './components/debug-mode-timeline-card/debug-mode-timeline-card.component';
import { DebugModeTimelineComponent } from './components/debug-mode-timeline/debug-mode-timeline.component';
import { DebugModeTimelineLegendComponent } from './components/debug-mode-timeline-legend/debug-mode-timeline-legend.component';
import { DebugModeInspectorTextComponent } from './components/debug-mode-inspector/debug-mode-inspector-text/debug-mode-inspector-text.component';
import { DebugModeTimelineOverviewComponent } from './components/debug-mode-timeline-overview/debug-mode-timeline-overview.component';
import { DebugModeTimelineHoverComponent } from './components/debug-mode-timeline-hover/debug-mode-timeline-hover.directive';

import { GioDiffModule } from '../../../shared/components/gio-diff/gio-diff.module';

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
    GioClipboardModule,
    MatCardModule,
    GioMonacoEditorModule,
  ],
  declarations: [
    DebugModeComponent,
    DebugModeInspectorComponent,
    DebugModeInspectorBodyComponent,
    DebugModeInspectorErrorComponent,
    DebugModeInspectorTableComponent,
    DebugModeInspectorTextComponent,
    DebugModeRequestComponent,
    DebugModeResponseComponent,
    DebugModeTimelineCardComponent,
    DebugModeTimelineComponent,
    DebugModeTimelineOverviewComponent,
    DebugModeTimelineLegendComponent,
    DebugModeTimelineHoverComponent,
  ],
  exports: [DebugModeComponent],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  providers: [TitleCasePipe],
})
export class DebugModeModule {}
