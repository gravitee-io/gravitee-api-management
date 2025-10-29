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
import { CommonModule, DATE_PIPE_DEFAULT_OPTIONS } from '@angular/common';
import { Injectable, NgModule } from '@angular/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { Observable } from 'rxjs/internal/Observable';
import { of } from 'rxjs/internal/observable/of';

import { DataResponse } from '../entities/common/data-response';
import { Configuration } from '../entities/configuration/configuration';
import { IdentityProvider } from '../entities/configuration/identity-provider';
import { ConfigService } from '../services/config.service';
import { IdentityProviderService } from '../services/identity-provider.service';
import {HttpClientTestingModule} from "@angular/common/http/testing";

export const TESTING_BASE_URL = 'http://localhost:8083/portal/environments/DEFAULT';
export const TESTING_ACTIVATED_ROUTE = {
  data: { breadcrumb: { alias: 'apiName' } },
  snapshot: { data: { breadcrumb: { alias: 'apiName' } } },
  params: of({ apiId: 'apiId' }),
  queryParams: of({}),
};

@Injectable()
export class ConfigServiceStub {
  private _configuration: Configuration = {
    portalNext: {
      banner: {
        enabled: true,
        title: 'Welcome to Gravitee Developer Portal!',
        subtitle: 'Great subtitle',
      },
    },
    authentication: {
      localLogin: {
        enabled: true,
      },
    },
  };

  get baseURL(): string {
    return TESTING_BASE_URL;
  }

  get configuration(): Configuration {
    return this._configuration ?? {};
  }

  set configuration(configuration: Configuration) {
    this._configuration = configuration;
  }
}

@Injectable()
export class IdentityProviderServiceStub {
  providers: IdentityProvider[] = [];

  getPortalIdentityProviders(): Observable<DataResponse<IdentityProvider[]>> {
    return of({ data: this.providers } as unknown as DataResponse<IdentityProvider[]>);
  }
  getPortalIdentityProvider(): Observable<IdentityProvider> {
    return of();
  }
}

@Injectable()
export class OAuthServiceStub {
  initCodeFlow() {
    // nothing to do
  }
  tryLoginCodeFlow(): Promise<void> {
    return Promise.resolve();
  }
  configure() {
    // nothing to do
  }
}

/**
 * To avoid error during unit tests that navigation happens outside the ngZone
 */
@NgModule()
export class TestRouterModule {
  constructor(_router: Router) {}
}

@NgModule({
  declarations: [],
  imports: [CommonModule, HttpClientTestingModule, NoopAnimationsModule, TestRouterModule],
  providers: [
    {
      provide: ConfigService,
      useClass: ConfigServiceStub,
    },
    {
      provide: OAuthService,
      useClass: OAuthServiceStub,
    },
    {
      provide: IdentityProviderService,
      useClass: IdentityProviderServiceStub,
    },
    {
      provide: ActivatedRoute,
      useValue: TESTING_ACTIVATED_ROUTE,
    },
    {
      provide: DATE_PIPE_DEFAULT_OPTIONS,
      useValue: {
        dateFormat: 'YYYY-MM-dd HH:mm:ss.SSS',
      },
    },
  ],
})
export class AppTestingModule {}
