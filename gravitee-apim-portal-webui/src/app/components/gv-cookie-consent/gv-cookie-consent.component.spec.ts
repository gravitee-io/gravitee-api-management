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
import { HttpClient } from '@angular/common/http';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { TranslateTestingModule } from '../../test/translate-testing-module';
import { GvCookieConsentComponent } from './gv-cookie-consent.component';
import '@gravitee/ui-components/wc/gv-button';

describe('GvCookieConsentComponent', () => {
  const createComponent = createComponentFactory({
    component: GvCookieConsentComponent,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    imports: [TranslateTestingModule, RouterTestingModule],
    providers: [mockProvider(HttpClient)],
  });

  let spectator: Spectator<GvCookieConsentComponent>;
  let component;

  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
