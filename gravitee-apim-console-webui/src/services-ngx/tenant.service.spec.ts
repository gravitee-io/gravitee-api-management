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

import { TenantService } from './tenant.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeTenant } from '../entities/tenant/tenant.fixture';
import { fakeNewTenant } from '../entities/tenant/newTenant.fixture';

describe('TenantService', () => {
  let httpTestingController: HttpTestingController;
  let tenantService: TenantService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    tenantService = TestBed.inject<TenantService>(TenantService);
  });

  describe('list', () => {
    it('should call the API', done => {
      const tenants = [fakeTenant()];

      tenantService.list().subscribe(response => {
        expect(response).toStrictEqual(tenants);
        done();
      });

      expectTenantsGetRequest(httpTestingController, tenants);
    });
  });

  describe('create', () => {
    it('should call the API', done => {
      const newTenants = [fakeNewTenant()];
      const createdTenants = [{ ...newTenants[0], id: 'createdTenant' }];

      tenantService.create(newTenants).subscribe(response => {
        expect(response).toStrictEqual(createdTenants);
        done();
      });

      const req = httpTestingController.expectOne({ method: 'POST', url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tenants` });
      expect(req.request.body).toStrictEqual(newTenants);
      req.flush(createdTenants);
    });
  });

  describe('update', () => {
    it('should call the API', done => {
      const tenants = [fakeTenant()];

      tenantService.update(tenants).subscribe(response => {
        expect(response).toStrictEqual(tenants);
        done();
      });

      const req = httpTestingController.expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tenants` });
      expect(req.request.body).toStrictEqual(tenants);
      req.flush(tenants);
    });
  });

  describe('delete', () => {
    it('should call the API', done => {
      tenantService.delete('tenant#1').subscribe(() => done());

      httpTestingController
        .expectOne({ method: 'DELETE', url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tenants/tenant#1` })
        .flush(null);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});

export function expectTenantsGetRequest(httpTestingController: HttpTestingController, tenants = [fakeTenant()]): void {
  httpTestingController.expectOne({ url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tenants`, method: 'GET' }).flush(tenants);
}
