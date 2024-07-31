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
import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Injectable, NgModule } from '@angular/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs/internal/observable/of';

import { Configuration } from '../entities/configuration/configuration';
import { ConfigService } from '../services/config.service';

export const TESTING_BASE_URL = 'http://localhost:8083/portal/environments/DEFAULT';
export const TESTING_ACTIVATED_ROUTE = {
  data: { breadcrumb: { alias: 'apiName' } },
  snapshot: { data: { breadcrumb: { alias: 'apiName' } } },
  params: of({ apiId: 'apiId' }),
};
@Injectable()
export class ConfigServiceStub {
  get baseURL(): string {
    return TESTING_BASE_URL;
  }

  get configuration(): Configuration {
    return {
      portalNext: {
        banner: {
          enabled: true,
          title: 'Welcome to Gravitee Developer Portal!',
          subtitle: 'Great subtitle',
        },
      },
    };
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
      provide: ActivatedRoute,
      useValue: TESTING_ACTIVATED_ROUTE,
    },
  ],
})
export class AppTestingModule {}
