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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';

import { ClientRegistrationProviderComponent } from './client-registration-provider.component';

import { UIRouterState, UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { ClientRegistrationProvidersModule } from '../client-registration-providers.module';
import {
  fakeClientRegistrationProvider,
  fakeNewClientRegistrationProvider,
} from '../../../../entities/client-registration-provider/clientRegistrationProvider.fixture';
import { GioPermissionService } from '../../../../shared/components/gio-permission/gio-permission.service';

describe('ClientRegistrationProvider', () => {
  const fakeAjsState = {
    go: jest.fn(),
  };
  let httpTestingController: HttpTestingController;
  const PROVIDER = fakeClientRegistrationProvider();

  let fixture: ComponentFixture<ClientRegistrationProviderComponent>;
  let loader: HarnessLoader;

  function initComponent(clientProviderId?: string) {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ClientRegistrationProvidersModule],
      providers: [
        { provide: UIRouterState, useValue: fakeAjsState },
        {
          provide: UIRouterStateParams,
          useValue: {
            id: clientProviderId,
          },
        },
        {
          provide: 'Constants',
          useValue: CONSTANTS_TESTING,
        },
        {
          provide: GioPermissionService,
          useValue: {
            hasAnyMatching: () => true,
          },
        },
      ],
    });

    fixture = TestBed.createComponent(ClientRegistrationProviderComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  }

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('should create', () => {
    beforeEach(() => {
      initComponent();
    });

    it('should init', async () => {
      expect(loader).toBeTruthy();
      expect(fixture.componentInstance.updateMode).toBeFalsy();
    });

    it('should not submit empty form', async () => {
      expect(loader).toBeTruthy();
      fixture.componentInstance.onSubmit();
    });

    it('should submit form', async () => {
      expect(loader).toBeTruthy();
      fixture.componentInstance.providerForm.setValue(fakeNewClientRegistrationProvider());
      fixture.componentInstance.onSubmit();

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/configuration/applications/registration/providers`,
      });
      req.flush({ data: PROVIDER });
    });
  });

  describe('should update', () => {
    beforeEach(() => {
      initComponent(PROVIDER.id);
      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.env.baseURL}/configuration/applications/registration/providers/${PROVIDER.id}`,
        })
        .flush({ data: PROVIDER });
      fixture.detectChanges();
    });

    it('should init', async () => {
      expect(loader).toBeTruthy();
      expect(fixture.componentInstance.updateMode).toBeTruthy();
    });

    it('should submit', async () => {
      expect(loader).toBeTruthy();
      fixture.componentInstance.onSubmit();
      const req = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.baseURL}/configuration/applications/registration/providers/${PROVIDER.id}`,
      });
      req.flush({ data: PROVIDER });
    });
  });
});
