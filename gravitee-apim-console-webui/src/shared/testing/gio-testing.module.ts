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
import { importProvidersFrom, NgModule, NgZone } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { Router } from '@angular/router';
import { MatMomentDateModule, provideMomentDateAdapter } from '@angular/material-moment-adapter';
import { GioFormJsonSchemaModule } from '@gravitee/ui-particles-angular';

import { Constants } from '../../entities/Constants';
import { GioFormJsonSchemaExtendedModule } from '../components/form-json-schema-extended/form-json-schema-extended.module';

export const CONSTANTS_TESTING: Constants = {
  org: {
    id: 'organization-id',
    baseURL: 'https://url.test:3000/management/organizations/DEFAULT',
    settings: {
      reCaptcha: { enabled: false },
      alert: {
        enabled: false,
      },
    },
    // FIXME: Fill missing fields
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    currentEnv: { id: 'DEFAULT' },
  },
  env: {
    baseURL: 'https://url.test:3000/management/organizations/DEFAULT/environments/DEFAULT',
    settings: {
      analytics: {
        clientTimeout: 50,
      },
      apiQualityMetrics: {
        enabled: false,
      },
    } as any,
    v2BaseURL: 'https://url.test:3000/management/v2/environments/DEFAULT',
  },
  v2BaseURL: 'https://url.test:3000/management/v2',
};

@NgModule({
  imports: [
    HttpClientTestingModule,
    RouterTestingModule.withRoutes([{ path: '**', redirectTo: '' }]),
    MatIconTestingModule,
    MatMomentDateModule,
    GioFormJsonSchemaExtendedModule,
  ],
  providers: [
    {
      provide: Constants,
      useValue: CONSTANTS_TESTING,
    },
    provideMomentDateAdapter(undefined, { useUtc: true }),
    importProvidersFrom(GioFormJsonSchemaModule),
  ],
})
export class GioTestingModule {
  constructor(
    private readonly ngZone: NgZone,
    private readonly router: Router,
  ) {
    ngZone.run(() => {
      router.initialNavigation();
    });
  }
}
