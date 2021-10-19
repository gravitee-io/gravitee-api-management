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
import { NgModule } from '@angular/core';

import { Constants } from '../../entities/Constants';

export const CONSTANTS_TESTING: Constants = {
  org: {
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
  },
};

@NgModule({
  imports: [HttpClientTestingModule],
  providers: [
    {
      provide: 'Constants',
      useValue: CONSTANTS_TESTING,
    },
  ],
})
export class GioHttpTestingModule {}
