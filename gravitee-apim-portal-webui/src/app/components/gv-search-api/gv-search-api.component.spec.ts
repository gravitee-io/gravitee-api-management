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
import { RouterTestingModule } from '@angular/router/testing';
import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { CurrentUserService } from '../../services/current-user.service';
import { TranslateTestingModule } from '../../test/translate-testing-module';

import { GvSearchApiComponent } from './gv-search-api.component';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

describe('GvSearchApiComponent', () => {
  const createComponent = createComponentFactory({
    component: GvSearchApiComponent,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    imports: [RouterTestingModule, TranslateTestingModule, HttpClientTestingModule],
    providers: [mockProvider(CurrentUserService)],
  });

  let spectator: Spectator<GvSearchApiComponent>;
  let component;

  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
