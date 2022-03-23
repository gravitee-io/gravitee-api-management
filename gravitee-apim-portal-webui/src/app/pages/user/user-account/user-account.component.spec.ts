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
import { ReactiveFormsModule } from '@angular/forms';
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { UserTestingModule } from '../../../test/user-testing-module';
import { SafePipe } from '../../../pipes/safe.pipe';

import { UserAccountComponent } from './user-account.component';

describe('UserAccountComponent', () => {
  const createComponent = createComponentFactory({
    component: UserAccountComponent,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    imports: [UserTestingModule, ReactiveFormsModule],
    declarations: [SafePipe],
  });

  let spectator: Spectator<UserAccountComponent>;
  let component;

  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
