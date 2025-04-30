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
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';
import { MatIconModule } from '@angular/material/icon';
import { GioClipboardModule, GioIconsModule } from '@gravitee/ui-particles-angular';
import { MatTreeModule } from '@angular/material/tree';
import { MatButtonModule } from '@angular/material/button';

import { DebugModeInspectorComponent } from './debug-mode-inspector.component';
import { DebugModeInspectorBodyComponent } from './debug-mode-inspector-body/debug-mode-inspector-body.component';
import { DebugModeInspectorErrorComponent } from './debug-mode-inspector-error/debug-mode-inspector-error.component';
import { DebugModeInspectorTableComponent } from './debug-mode-inspector-table/debug-mode-inspector-table.component';
import { DebugModeInspectorTextComponent } from './debug-mode-inspector-text/debug-mode-inspector-text.component';

import { fakeErrorRequestDebugStep, fakeRequestDebugStep } from '../../models/DebugStep.fixture';
import { GioDiffModule } from '../../../../../shared/components/gio-diff/gio-diff.module';

export default {
  title: 'APIM / Policy Studio / Debug / Components / Inspector ',
  component: DebugModeInspectorComponent,
  decorators: [
    moduleMetadata({
      declarations: [
        DebugModeInspectorBodyComponent,
        DebugModeInspectorErrorComponent,
        DebugModeInspectorTableComponent,
        DebugModeInspectorTextComponent,
      ],
      imports: [
        CommonModule,
        BrowserAnimationsModule,
        MatIconModule,
        GioIconsModule,
        MatTreeModule,
        MatButtonModule,
        GioDiffModule,
        GioClipboardModule,
      ],
    }),
  ],
  parameters: {
    layout: 'fullscreen',
  },
  render: ({ inputDebugStep, outputDebugStep }) => ({
    props: { inputDebugStep, outputDebugStep },
    template: `
      <div style="height:100vh">
        <debug-mode-inspector [inputDebugStep]="inputDebugStep" [outputDebugStep]="outputDebugStep">
        </debug-mode-inspector>
      </div>
    `,
  }),
} as Meta;

export const Empty: StoryObj = {};

export const ResponseSuccess: StoryObj = {
  args: {
    inputDebugStep: fakeRequestDebugStep(),
    outputDebugStep: fakeRequestDebugStep({
      output: {
        path: '/fake/api/updated',
        contextPath: '/context/path',
        headers: {
          'content-length': ['3476'],
          'x-gravitee-api': ['planets'],
          'Sozu-Id': [''],
        },
        attributes: {
          bool: false,
          numeric: 3,
        },
        body: `{
  "foo": "bar",
  "icon": "🥷",
}`,
      },
    }),
  },
};

export const ResponseError: StoryObj = {
  args: {
    inputDebugStep: fakeRequestDebugStep(),
    outputDebugStep: fakeErrorRequestDebugStep(),
  },
};
