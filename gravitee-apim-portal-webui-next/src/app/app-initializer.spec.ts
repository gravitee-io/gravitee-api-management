/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { firstValueFrom } from 'rxjs';

import { initApp } from './app-initializer';
import { AuthService } from '../services/auth.service';
import { ConfigService } from '../services/config.service';
import { CurrentUserService } from '../services/current-user.service';
import { IdentityProviderService } from '../services/identity-provider.service';
import { PortalNavigationItemsService } from '../services/portal-navigation-items.service';
import { ThemeService } from '../services/theme.service';
import { IdentityProviderServiceStub, OAuthServiceStub } from '../testing/app-testing.module';

describe('initApp', () => {
  const bootstrapUrl = 'https://apim.example.com/portal/ui/bootstrap';
  let httpTestingController: HttpTestingController;
  let navigate: jest.SpyInstance;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: OAuthService, useClass: OAuthServiceStub },
        { provide: IdentityProviderService, useClass: IdentityProviderServiceStub },
      ],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    navigate = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  function runInitializer(): Promise<unknown> {
    const initializerFn = initApp(
      TestBed.inject(AuthService),
      TestBed.inject(ConfigService),
      TestBed.inject(ThemeService),
      TestBed.inject(CurrentUserService),
      TestBed.inject(PortalNavigationItemsService),
      TestBed.inject(Router),
    );
    const initialized = firstValueFrom(initializerFn());
    httpTestingController.expectOne('./assets/config.json').flush({ baseURL: 'https://apim.example.com/portal' });
    return initialized;
  }

  it('should navigate to /503 with a cloneable state when the bootstrap call fails on the network', async () => {
    const initialized = runInitializer();

    // What the browser reports when the response has no CORS headers: status 0 and a ProgressEvent as body
    httpTestingController.expectOne(bootstrapUrl).error(new ProgressEvent('error'), { status: 0, statusText: 'Unknown Error' });
    await initialized;

    expect(navigate).toHaveBeenCalledWith(['/503'], { state: { status: 0 } });
  });

  it('should navigate to /503 with the API errors when the bootstrap call fails because of maintenance mode', async () => {
    const initialized = runInitializer();

    httpTestingController
      .expectOne(bootstrapUrl)
      .flush(
        { errors: [{ code: 'errors.maintenance.mode', message: 'Portal is under maintenance', status: '503' }] },
        { status: 503, statusText: 'Service Unavailable' },
      );
    await initialized;

    expect(navigate).toHaveBeenCalledWith(['/503'], {
      state: { status: 503, errors: [{ code: 'errors.maintenance.mode', message: 'Portal is under maintenance', status: '503' }] },
    });
  });
});
