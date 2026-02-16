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
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { GioFormUserAutocompleteComponent } from './gio-form-user-autocomplete.component';
import { GioFormUserAutocompleteModule } from './gio-form-user-autocomplete.module';

import { UsersService } from '../../../services-ngx/users.service';
import { fakeSearchableUser } from '../../../entities/user/searchableUser.fixture';

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

export default {
  title: 'Shared / Form user autocomplete',
  component: GioFormUserAutocompleteComponent,
  decorators: [
    moduleMetadata({
      imports: [GioFormUserAutocompleteModule, BrowserAnimationsModule, ReactiveFormsModule],
      providers: [
        {
          provide: UsersService,
          useValue: {
            search: () => of(searchableUsers),
            getUserAvatar: () => 'https://i.pravatar.cc/100',
            get: (id: string) => of(searchableUsers.find(user => user.id === id)),
          },
        },
      ],
    }),
  ],
  argTypes: {
    selectedUserId: {
      control: { type: 'text' },
    },
    disabled: {
      defaultValue: false,
      control: { type: 'boolean' },
    },
  },
  render: ({ selectedUserId, disabled }) => {
    const userControl = new FormControl({ value: searchableUsers.find(u => u.id === selectedUserId), disabled });

    userControl.valueChanges.subscribe(value => {
      action('UserId')(value);
    });

    userControl.statusChanges.subscribe(value => {
      action('UserId')(value);
    });

    return {
      template: `
        <gio-form-user-autocomplete style="width: 100%" [formControl]="userControl"></gio-form-user-autocomplete>
        <br>
        STATUS: {{ userControl.status }}<br>
        TOUCHED: {{ userControl.touched }}<br>
        DIRTY: {{ userControl.dirty }}<br>
        VALUE: {{ userControl.value  | json}}<br>
      `,
      props: {
        userControl,
      },
    };
  },
} as Meta;

export const Default: StoryObj = {};

export const WithValue: StoryObj = {
  args: {
    selectedUserId: searchableUsers[0].id,
  },
};
export const Disabled: StoryObj = {
  args: {
    selectedUserId: searchableUsers[0].id,
    disabled: true,
  },
};
