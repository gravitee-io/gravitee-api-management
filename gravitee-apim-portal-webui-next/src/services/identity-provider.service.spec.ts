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
import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { IdentityProviderService } from './identity-provider.service';
import { DataResponse } from '../entities/common/data-response';
import { IdentityProvider } from '../entities/configuration/identity-provider';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('IdentityProviderService', () => {
  let service: IdentityProviderService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
      providers: [IdentityProviderService],
    });
    service = TestBed.inject(IdentityProviderService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  it('should load identity provider by id', done => {
    const provider = { id: 'google' };
    service.getPortalIdentityProvider(provider.id).subscribe(() => done());
    httpTestingController
      .expectOne(`${TESTING_BASE_URL}/configuration/identities/${encodeURIComponent(String(provider.id))}`)
      .flush(provider);
  });

  it('should load identity provider by id', done => {
    const response: DataResponse<IdentityProvider> = {
      data: [{ id: 'google' }],
    };
    service.getPortalIdentityProviders().subscribe(() => done());
    httpTestingController.expectOne(`${TESTING_BASE_URL}/configuration/identities`).flush(response);
  });
});
