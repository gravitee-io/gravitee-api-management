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
import { HttpTestingController } from '@angular/common/http/testing';

import { IntegrationsService } from './integrations.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { CreateIntegrationPayload, Integration } from '../management/integrations/integrations.model';

describe('IntegrationsService', () => {
  const url = `${CONSTANTS_TESTING.env.v2BaseURL}/integrations`;
  let service: IntegrationsService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });
    service = TestBed.inject(IntegrationsService);

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<IntegrationsService>(IntegrationsService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('get integrations', () => {
    it('should call API', () => {
      const fakeData: Integration[] = null;

      service.getIntegrations(1, 10).subscribe(res => {
        expect(res).toMatchObject(fakeData);
      });

      httpTestingController.expectOne({ method: 'GET', url: url + '?page=1&perPage=10' }).flush(fakeData);
    });
  });

  describe('create', () => {
    it('should call API', done => {
      const fakeData: CreateIntegrationPayload = {
        name: 'test_name',
        description: 'test_description',
        provider: 'test_provider',
      };

      service.createIntegration(fakeData).subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({ method: 'POST', url: url });
      req.flush(null);
      expect(req.request.body).toEqual(fakeData);
    });
  });
});
