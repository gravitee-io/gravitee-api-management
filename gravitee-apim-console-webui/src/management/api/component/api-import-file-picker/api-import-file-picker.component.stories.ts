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
import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';
import { GioIconsModule } from '@gravitee/ui-particles-angular';

import { ApiImportFilePickerComponent } from './api-import-file-picker.component';

export default {
  title: 'Api / Import / File picker',
  component: ApiImportFilePickerComponent,
  decorators: [
    moduleMetadata({
      imports: [ApiImportFilePickerComponent, GioIconsModule],
    }),
  ],
} as Meta;

export const Default: StoryObj = {
  args: {
    allowedFileExtensions: ['json', 'yaml', 'yml'],
  },
  render: ({ allowedFileExtensions }) => {
    return {
      props: { allowedFileExtensions },
      template: `<api-import-file-picker [allowedFileExtensions]="allowedFileExtensions"></api-import-file-picker>`,
    };
  },
};

export const WithCustomContent: StoryObj = {
  args: {
    allowedFileExtensions: ['json', 'yaml', 'yml'],
  },
  render: ({ allowedFileExtensions }) => {
    return {
      props: { allowedFileExtensions },
      template: `
        <api-import-file-picker [allowedFileExtensions]="allowedFileExtensions">
          <div style="display: flex; flex-direction: column; align-items: center">
              <mat-icon svgIcon="gio:file-plus" style="height: 50px; width: 50px"></mat-icon>
              <p class="mat-body-strong">
                Drag and drop a file to upload it. <br />
                Alternatively, click here to choose a file.
              </p>
              <p>
                Supported file formats: yml, yaml, json<br />
                Supported API Definition: V2, V4.
              </p>
          </div>
        </api-import-file-picker>`,
    };
  },
};
