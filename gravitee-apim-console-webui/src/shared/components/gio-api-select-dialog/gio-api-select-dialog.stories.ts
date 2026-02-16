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
import { applicationConfig, Meta, moduleMetadata, StoryObj } from '@storybook/angular';
import { action } from 'storybook/actions';
import { of } from 'rxjs';
import { Component } from '@angular/core';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { tap } from 'rxjs/operators';

import { GioApiSelectDialogComponent, GioApiSelectDialogData, GioApiSelectDialogResult } from './gio-api-select-dialog.component';

import { ApisResponse, fakeApiV4 } from '../../../entities/management-api-v2';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';

const searchApisResponse: ApisResponse = {
  data: [
    fakeApiV4({ id: 'api-1', name: 'API 1' }),
    fakeApiV4({ id: 'api-2', name: 'API 2' }),
    fakeApiV4({ id: 'api-3', name: 'API 3' }),
    fakeApiV4({ id: 'api-4', name: 'API 4' }),
    fakeApiV4({ id: 'api-5', name: 'API 5' }),
  ],
  pagination: {
    totalCount: 5,
  },
};

@Component({
  selector: `open-dialog-story`,
  template: `<button id="open-dialog-story" (click)="openDialog()">Open dialog</button>`,
})
class OpenDialogStoryComponent {
  constructor(private readonly matDialog: MatDialog) {}

  openDialog() {
    this.matDialog
      .open<GioApiSelectDialogComponent, GioApiSelectDialogData, GioApiSelectDialogResult>(GioApiSelectDialogComponent, {
        width: '500px',
        data: {
          title: 'Select an API',
        },
        role: 'alertdialog',
        id: 'openDialogStoryDialog',
      })
      .afterClosed()
      .pipe(
        tap(selected => {
          action('selected')(selected);
        }),
      )
      .subscribe();
  }
}

export default {
  title: 'Shared / API Select Dialog',
  component: GioApiSelectDialogComponent,
  decorators: [
    moduleMetadata({
      declarations: [OpenDialogStoryComponent],
      imports: [GioApiSelectDialogComponent, MatDialogModule, BrowserAnimationsModule],
    }),
    applicationConfig({
      providers: [{ provide: ApiV2Service, useValue: { search: () => of(searchApisResponse) } }],
    }),
  ],

  render: () => ({
    template: `<open-dialog-story></open-dialog-story>`,
  }),
} as Meta;

export const Default: StoryObj = {
  play: context => {
    const button = context.canvasElement.querySelector('#open-dialog-story') as HTMLButtonElement;
    button.click();
  },
};
