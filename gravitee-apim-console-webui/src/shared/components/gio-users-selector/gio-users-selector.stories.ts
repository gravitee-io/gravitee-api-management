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
import { action } from 'storybook/actions';
import { of } from 'rxjs';
import { Component } from '@angular/core';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { tap } from 'rxjs/operators';

import { GioUsersSelectorComponent, GioUsersSelectorData } from './gio-users-selector.component';
import { GioUsersSelectorModule } from './gio-users-selector.module';

import { UsersService } from '../../../services-ngx/users.service';
import { fakeSearchableUser } from '../../../entities/user/searchableUser.fixture';
import { SearchableUser } from '../../../entities/user/searchableUser';

const searchableUsers = [
  fakeSearchableUser(),
  fakeSearchableUser({ displayName: 'Aquaman', id: '15873e92ee2112001ade0917' }),
  fakeSearchableUser({ displayName: 'Flash', id: '15222e92ee2112001ade0923' }),
  fakeSearchableUser({ displayName: 'Flash1', id: '152a22e92ee2112001ade0923' }),
  fakeSearchableUser({ displayName: 'Flash2', id: '1522w2e92ee2112001ade0923' }),
  fakeSearchableUser({ displayName: 'Flash3', id: '15222e92wee2112001ade0923' }),
  fakeSearchableUser({ displayName: 'Flash4', id: '15222e15223ee15224q15225ade15226' }),
  fakeSearchableUser({ displayName: 'Flash5', id: '15227e15228ee15229q15230ade15231' }),
  fakeSearchableUser({ displayName: 'Flash6', id: '15232e15233ee15234q15235ade15236' }),
  fakeSearchableUser({ displayName: 'Flash7', id: '15237e15238ee15239q15240ade15241' }),
  fakeSearchableUser({ displayName: 'Flash8', id: '15222e92ee211q2001ade0923' }),
  fakeSearchableUser({ displayName: 'Filtered Flash', id: 'flash' }),
];

@Component({
  selector: `users-selector-story`,
  template: `<button id="open-users-selector" (click)="openUserSelector()">Open users selector</button>`,
})
class UsersSelectorStoryComponent {
  constructor(private readonly matDialog: MatDialog) {}

  openUserSelector() {
    this.matDialog
      .open<GioUsersSelectorComponent, GioUsersSelectorData, SearchableUser[]>(GioUsersSelectorComponent, {
        width: '500px',
        data: {
          userFilterPredicate: user => user.id !== 'flash',
        },
        role: 'alertdialog',
        id: 'deleteIdentityProviderConfirmDialog',
      })
      .afterClosed()
      .pipe(
        tap(selectedUsers => {
          action('selectedUsers')(selectedUsers);
        }),
      )
      .subscribe();
  }
}

export default {
  title: 'Shared / Users Selector',
  component: GioUsersSelectorComponent,
  decorators: [
    moduleMetadata({
      declarations: [UsersSelectorStoryComponent],
      imports: [GioUsersSelectorModule, MatDialogModule, BrowserAnimationsModule],
      providers: [
        { provide: UsersService, useValue: { search: () => of(searchableUsers), getUserAvatar: () => 'https://i.pravatar.cc/100' } },
      ],
    }),
  ],
  render: () => ({
    template: `<users-selector-story></users-selector-story>`,
  }),
} as Meta;

export const Default: StoryObj = {
  play: context => {
    const button = context.canvasElement.querySelector('#open-users-selector') as HTMLButtonElement;
    button.click();
  },
};
