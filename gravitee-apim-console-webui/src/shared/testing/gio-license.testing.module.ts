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
import { NgModule } from '@angular/core';
import { of } from 'rxjs';

import { GioLicenseService } from '../components/gio-license/gio-license.service';

@NgModule({
  imports: [],
  providers: [
    {
      provide: GioLicenseService,
      useValue: {
        isMissingFeature$: () => of(true),
        isMissingPack$: () => of(true),
        getFeatureInfo: () => ({}),
        getTrialURL: () => '',
      },
    },
  ],
})
export class GioLicenseTestingModule {
  public static with(license: boolean) {
    return {
      ngModule: GioLicenseTestingModule,
      providers: [
        {
          provide: GioLicenseService,
          useValue: {
            isMissingFeature$: () => of(!license),
            isMissingPack$: () => of(license),
            getFeatureInfo: () => ({}),
            getTrialURL: () => '',
          },
        },
      ],
    };
  }
}
