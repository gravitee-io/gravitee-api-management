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
import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AimA2aProxyService } from './aim-a2a-proxy.service';

import { GioTestingModule } from '../shared/testing';

describe('AimA2aProxyService', () => {
  let httpTestingController: HttpTestingController;
  let service: AimA2aProxyService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject(AimA2aProxyService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should get an A2A proxy from the AIM module', done => {
    service.getA2aProxy('api-1').subscribe(response => {
      expect(response.agentId).toBe('catalog-agent-1');
      done();
    });

    const req = httpTestingController.expectOne(
      'https://url.test:3000/gamma/organizations/organization-id/environments/DEFAULT/modules/aim/a2a-proxies/api-1',
    );
    expect(req.request.method).toEqual('GET');
    req.flush({ agentId: 'catalog-agent-1' });
  });
});
