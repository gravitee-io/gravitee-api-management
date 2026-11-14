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
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { ApplicationCreationStep5Component } from './application-creation-step5.component';

describe('ApplicationCreationStep5Component', () => {
  const createComponent = createComponentFactory({
    component: ApplicationCreationStep5Component,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
  });

  let spectator: Spectator<ApplicationCreationStep5Component>;
  let component;

  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show the error message returned by the server when the creation fails', () => {
    const message = 'Unexpected response from OIDC Registration endpoint: error[registration_forbidden]';
    spectator.setInput({ creationError: true, creationErrorMessage: message });

    expect(spectator.query('[data-testid="application-creation-error"]')).toHaveText(message);
  });

  it('should show only the generic error when the server returned no message', () => {
    spectator.setInput({ creationError: true, creationErrorMessage: null });

    expect(spectator.query('[data-testid="application-creation-error"]')).toBeTruthy();
    expect(spectator.query('[data-testid="application-creation-error-message"]')).toBeNull();
  });
});
