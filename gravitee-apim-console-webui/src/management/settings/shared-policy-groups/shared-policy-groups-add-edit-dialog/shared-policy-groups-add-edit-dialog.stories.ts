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
import { Meta, StoryObj } from '@storybook/angular';
import { Component } from '@angular/core';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { action } from '@storybook/addon-actions';

import {
  SharedPolicyGroupsAddEditDialogComponent,
  SharedPolicyGroupAddEditDialogData,
  SharedPolicyGroupAddEditDialogResult,
} from './shared-policy-groups-add-edit-dialog.component';

@Component({
  selector: 'story-component',
  template: `<button id="open-dialog" (click)="open()">Open dialog</button>`,
  imports: [SharedPolicyGroupsAddEditDialogComponent, MatDialogModule, MatIconTestingModule],
  standalone: true,
})
class StoryDialogComponent {
  constructor(private readonly matDialog: MatDialog) {}

  public open(data?: SharedPolicyGroupAddEditDialogData) {
    return this.matDialog
      .open<SharedPolicyGroupsAddEditDialogComponent, SharedPolicyGroupAddEditDialogData, SharedPolicyGroupAddEditDialogResult>(
        SharedPolicyGroupsAddEditDialogComponent,
        {
          data,
          role: 'dialog',
          id: 'test-story-dialog',
        },
      )
      .afterClosed()
      .subscribe((result) => {
        action('Close')({ result });
      });
  }
}

export default {
  title: 'SharedPolicyGroupsAddEditDialogComponent story',
  component: StoryDialogComponent,
} as Meta;

export const Default: StoryObj = {
  play: (context) => {
    const button = context.canvasElement.querySelector('#open-dialog') as HTMLButtonElement;
    button.click();
  },
};
