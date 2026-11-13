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
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { throwError } from 'rxjs';

import { ApplicationService } from '../../../../../projects/portal-webclient-sdk/src/lib';

import { ApplicationCreationComponent } from './application-creation.component';

describe('ApplicationCreationComponent registration error', () => {
  const providerMessage =
    'Unexpected error while registering client: Unexpected response from OIDC Registration endpoint: error[registration_forbidden] description[Not allowed to access to this resource.]';

  const createComponent = createComponentFactory({
    component: ApplicationCreationComponent,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    imports: [HttpClientTestingModule, RouterTestingModule, FormsModule, ReactiveFormsModule],
    providers: [
      { provide: ActivatedRoute, useValue: { snapshot: { data: { enabledApplicationTypes: [] }, queryParamMap: convertToParamMap({}) } } },
    ],
  });

  let spectator: Spectator<ApplicationCreationComponent>;
  let component: ApplicationCreationComponent;

  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
  });

  function failApplicationCreationWith(error: unknown) {
    jest
      .spyOn(spectator.inject(ApplicationService), 'createApplication')
      .mockReturnValue(throwError(() => new HttpErrorResponse({ status: 400, error })) as never);
  }

  async function createApp() {
    await spectator.fixture.whenStable();
    component.applicationForm.setValue({
      name: 'backend-app',
      description: 'backend-description',
      domain: null,
      picture: null,
      settings: {
        oauth: {
          redirect_uris: [],
          grant_types: ['client_credentials'],
          application_type: 'backend_to_backend',
          additionalClientMetadata: [],
        },
        tls: { client_certificate: null },
      },
    });
    component.createApp();
    await new Promise(resolve => setTimeout(resolve, 0));
  }

  it('should show the error message of the client registration provider when it refuses the registration', async () => {
    failApplicationCreationWith({
      errors: [{ status: '400', message: providerMessage, code: 'errors.clientRegistrationProvider.exception' }],
    });

    await createApp();

    expect(component.creationError).toBe(true);
    expect(component.creationErrorMessage).toEqual(providerMessage);
  });

  it('should keep the generic creation error when the response carries no message', async () => {
    failApplicationCreationWith(null);

    await createApp();

    expect(component.creationError).toBe(true);
    expect(component.creationErrorMessage).toBeNull();
  });

  it('should forget the previous error message when the creation is retried', async () => {
    failApplicationCreationWith({
      errors: [{ status: '400', message: providerMessage, code: 'errors.clientRegistrationProvider.exception' }],
    });
    await createApp();

    failApplicationCreationWith(null);
    await createApp();

    expect(component.creationErrorMessage).toBeNull();
  });
});
