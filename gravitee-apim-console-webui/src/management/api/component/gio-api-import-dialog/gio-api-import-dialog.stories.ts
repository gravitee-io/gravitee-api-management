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
import { Component, Input } from '@angular/core';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';
import { action } from 'storybook/actions';
import { tap } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { of } from 'rxjs';

import { GioApiImportDialogComponent, GioApiImportDialogData } from './gio-api-import-dialog.component';
import { GioApiImportDialogModule } from './gio-api-import-dialog.module';

import { PolicyListItem } from '../../../../entities/policy';
import { ApiService } from '../../../../services-ngx/api.service';

@Component({
  selector: 'gio-api-import-dialog-story',
  template: `<button id="open-dialog" (click)="openDialog()">Open Api Import</button>`,
})
class ApiImportDialogStoryComponent {
  @Input() public policies?: PolicyListItem[];
  @Input() public apiId?: string;

  constructor(private readonly matDialog: MatDialog) {}

  public openDialog() {
    this.matDialog
      .open<GioApiImportDialogComponent, GioApiImportDialogData, boolean>(GioApiImportDialogComponent, {
        data: {
          policies: this.policies,
          apiId: this.apiId,
        },
        role: 'alertdialog',
        id: 'dialog',
      })
      .afterClosed()
      .pipe(
        tap(confirmed => {
          action('confirmed?')(confirmed);
        }),
      )
      .subscribe();
  }
}

export default {
  title: 'Api / Import / V2 Dialog',
  component: GioApiImportDialogComponent,
  decorators: [
    moduleMetadata({
      declarations: [ApiImportDialogStoryComponent],
      imports: [GioApiImportDialogModule, MatButtonModule, MatDialogModule, BrowserAnimationsModule],
      providers: [
        {
          provide: ApiService,
          useValue: {
            importApiDefinition: (...attr) => {
              action('importApiDefinition')(attr);
              return of({});
            },
            importSwaggerApi: (...attr) => {
              action('importSwaggerApi')(attr);

              return of();
            },
          },
        },
      ],
    }),
  ],
  argTypes: {
    policies: {
      type: { name: 'object', value: {} },
    },
  },
  render: args => ({
    template: `<gio-api-import-dialog-story [policies]="policies" [apiId]="apiId"></gio-api-import-dialog-story>`,
    props: { ...args },
  }),
  parameters: {
    chromatic: { delay: 1000 },
  },
} as Meta;

export const Default: StoryObj = {
  play: context => {
    const button = context.canvasElement.querySelector('#open-dialog') as HTMLButtonElement;
    button.click();
  },
};

Default.args = {
  policies: [
    {
      id: 'json-validation',
      name: 'JSON Validation',
      onRequest: false,
      onResponse: false,
    },
    {
      id: 'mock',
      name: 'Mock',
      onRequest: false,
      onResponse: false,
    },
    {
      id: 'rest-to-soap',
      name: 'Rest to SOAP Transformer',
      onRequest: false,
      onResponse: false,
    },
    {
      id: 'policy-request-validation',
      name: 'Validate Request',
      onRequest: false,
      onResponse: false,
    },
    {
      id: 'xml-validation',
      name: 'XML Validation',
      onRequest: false,
      onResponse: false,
    },
  ],
};

export const UpdateMode: StoryObj = {
  play: context => {
    const button = context.canvasElement.querySelector('#open-dialog') as HTMLButtonElement;
    button.click();
  },
};
UpdateMode.args = {
  apiId: 'api-id',
};
