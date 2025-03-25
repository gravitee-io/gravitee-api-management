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
import { TestBed } from '@angular/core/testing';
import { LICENSE_CONFIGURATION_TESTING } from '@gravitee/ui-particles-angular';

import { ApiV4MenuService } from './api-v4-menu.service';

import { GioTestingModule } from '../../../shared/testing';

describe('ApiV4MenuService', () => {
  let service: ApiV4MenuService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ApiV4MenuService, { provide: 'LicenseConfiguration', useValue: LICENSE_CONFIGURATION_TESTING }],
      imports: [GioTestingModule],
    });
    service = TestBed.inject(ApiV4MenuService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
