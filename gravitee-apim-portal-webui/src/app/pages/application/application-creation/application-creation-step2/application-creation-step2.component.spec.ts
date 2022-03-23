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
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { ApplicationCreationStep2Component } from './application-creation-step2.component';

describe('ApplicationCreationStep2Component', () => {
  const createComponent = createComponentFactory({
    component: ApplicationCreationStep2Component,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    imports: [HttpClientTestingModule, RouterTestingModule, FormsModule, ReactiveFormsModule],
  });

  let spectator: Spectator<ApplicationCreationStep2Component>;
  let component;

  const allowedTypes = [
    {
      id: 'simple',
      name: 'Simple',
      description: 'A hands-free application. Using this type, you will be able to define the client_id by your own.',
      requires_redirect_uris: false,
      allowed_grant_types: [],
      default_grant_types: [],
      mandatory_grant_types: [],
    },
    {
      id: 'browser',
      name: 'SPA',
      description: 'Angular, React, Ember, ...',
      requires_redirect_uris: true,
      allowed_grant_types: [
        {
          code: 'authorization_code',
          type: 'authorization_code',
          name: 'Authorization Code',
          response_types: ['code'],
        },
        {
          code: 'implicit',
          type: 'implicit',
          name: 'Implicit',
          response_types: ['token', 'id_token'],
        },
      ],
      default_grant_types: [
        {
          code: 'implicit',
          type: 'implicit',
          name: 'Implicit',
          response_types: ['token', 'id_token'],
        },
      ],
      mandatory_grant_types: [],
    },
  ].map(o => ({ ...o, title: o.name, icon: '' }));

  beforeEach(() => {
    spectator = createComponent({
      props: {
        allowedTypes,
      },
    });
    component = spectator.component;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
