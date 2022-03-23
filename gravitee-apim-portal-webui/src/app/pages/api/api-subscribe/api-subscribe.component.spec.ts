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
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ApiStatesPipe } from '../../../pipes/api-states.pipe';
import { ApiLabelsPipe } from '../../../pipes/api-labels.pipe';
import { TranslateTestingModule } from '../../../test/translate-testing-module';

import { ApiSubscribeComponent } from './api-subscribe.component';

describe('ApiSubscribeComponent', () => {
  const createComponent = createComponentFactory({
    component: ApiSubscribeComponent,
    imports: [HttpClientTestingModule, RouterTestingModule, TranslateTestingModule, FormsModule, ReactiveFormsModule],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    providers: [ApiStatesPipe, ApiLabelsPipe],
  });

  let spectator: Spectator<ApiSubscribeComponent>;
  let component;

  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
