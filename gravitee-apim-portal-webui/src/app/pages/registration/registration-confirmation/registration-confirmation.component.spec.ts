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
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { NotificationService } from '../../../services/notification.service';
import { TokenService } from '../../../services/token.service';

import { RegistrationConfirmationComponent } from './registration-confirmation.component';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

describe('RegistrationConfirmationComponent', () => {
  const createComponent = createComponentFactory({
    component: RegistrationConfirmationComponent,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    imports: [RouterTestingModule, FormsModule, ReactiveFormsModule, HttpClientTestingModule],
    providers: [
      mockProvider(NotificationService),
      mockProvider(TokenService, {
        parseToken: () => ({
          firstName: 'foo',
          lastName: 'bar',
          email: 'foo@bar',
        }),
      }),
    ],
  });

  let spectator: Spectator<RegistrationConfirmationComponent>;
  let component;

  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
