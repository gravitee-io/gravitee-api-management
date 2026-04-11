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

import { ApiProductV2Service } from './api-product-v2.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { AddMember, Member, MembersResponse, UpdateMember } from '../entities/management-api-v2';
import { ApiProduct, CreateApiProduct, UpdateApiProduct } from '../entities/management-api-v2/api-product';

function fakeApiProduct(overrides?: Partial<ApiProduct>): ApiProduct {
  return {
    id: 'api-product-1',
    name: 'Payments API Product',
    version: '1.0',
    description: 'Group of payment APIs',
    apiIds: ['api-1', 'api-2'],
    primaryOwner: { displayName: 'Jane Doe', id: 'user-1', email: 'jane@example.com' } as any,
    ...overrides,
  };
}

describe('ApiProductV2Service', () => {
  let httpTestingController: HttpTestingController;
  let apiProductV2Service: ApiProductV2Service;

  const baseURL = CONSTANTS_TESTING.env.v2BaseURL;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    apiProductV2Service = TestBed.inject(ApiProductV2Service);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('create', () => {
    it('should call the API', done => {
      const createApiProduct: CreateApiProduct = {
        name: 'New API Product',
        version: '1.0',
        description: 'Description',
        apiIds: ['api-1'],
      };

      apiProductV2Service.create(createApiProduct).subscribe(apiProduct => {
        expect(apiProduct.name).toEqual(createApiProduct.name);
        expect(apiProduct.version).toEqual(createApiProduct.version);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products`,
        method: 'POST',
      });

      expect(req.request.body).toEqual(createApiProduct);
      req.flush(fakeApiProduct(createApiProduct));
    });
  });

  describe('list', () => {
    it('should call the API with default page and perPage', done => {
      const response = { data: [fakeApiProduct()], pagination: { totalCount: 1 } };

      apiProductV2Service.list().subscribe(result => {
        expect(result.data).toHaveLength(1);
        expect(result.data?.[0].name).toEqual('Payments API Product');
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products?page=1&perPage=10`,
        method: 'GET',
      });

      req.flush(response);
    });

    it('should call the API with custom page and perPage', done => {
      const response = { data: [fakeApiProduct()], pagination: { totalCount: 1 } };

      apiProductV2Service.list(2, 25).subscribe(result => {
        expect(result.data).toHaveLength(1);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products?page=2&perPage=25`,
        method: 'GET',
      });

      req.flush(response);
    });
  });

  describe('search', () => {
    it('should call the API with default params and empty body', done => {
      const response = { data: [fakeApiProduct()], pagination: { totalCount: 1 } };

      apiProductV2Service.search().subscribe(result => {
        expect(result.data).toHaveLength(1);
        expect(result.data?.[0].name).toEqual('Payments API Product');
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products/_search?page=1&perPage=10`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({});
      req.flush(response);
    });

    it('should call the API with query and sortBy', done => {
      const response = { data: [fakeApiProduct()], pagination: { totalCount: 1 } };

      apiProductV2Service.search({ query: 'Payments' }, '-name', 2, 25).subscribe(result => {
        expect(result.data).toHaveLength(1);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products/_search?page=2&perPage=25&sortBy=-name`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({ query: 'Payments' });
      req.flush(response);
    });

    it('should call the API with ids filter', done => {
      const response = { data: [fakeApiProduct()], pagination: { totalCount: 1 } };

      apiProductV2Service.search({ ids: ['product-1', 'product-2'] }, 'name', 1, 10).subscribe(result => {
        expect(result.data).toHaveLength(1);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products/_search?page=1&perPage=10&sortBy=name`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({ ids: ['product-1', 'product-2'] });
      req.flush(response);
    });

    it('should call the API with sortBy version', done => {
      const response = { data: [fakeApiProduct()], pagination: { totalCount: 1 } };

      apiProductV2Service.search({}, 'version', 1, 10).subscribe(result => {
        expect(result.data).toHaveLength(1);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products/_search?page=1&perPage=10&sortBy=version`,
        method: 'POST',
      });
      req.flush(response);
    });

    it('should not send invalid sortBy to the backend', done => {
      const response = { data: [fakeApiProduct()], pagination: { totalCount: 1 } };

      apiProductV2Service.search({}, 'invalidSort' as any, 1, 10).subscribe(result => {
        expect(result.data).toHaveLength(1);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products/_search?page=1&perPage=10`,
        method: 'POST',
      });
      expect(req.request.params.has('sortBy')).toBe(false);
      req.flush(response);
    });
  });

  describe('get', () => {
    it('should call the API', done => {
      const fakeProduct = fakeApiProduct({ id: 'product-123' });

      apiProductV2Service.get('product-123').subscribe(apiProduct => {
        expect(apiProduct.id).toEqual('product-123');
        expect(apiProduct.name).toEqual(fakeProduct.name);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products/product-123`,
        method: 'GET',
      });

      req.flush(fakeProduct);
    });
  });

  describe('getApis', () => {
    it('should call the API with default page and perPage', done => {
      const response = { data: [], pagination: { totalCount: 0, page: 1, perPage: 10 }, links: {} };

      apiProductV2Service.getApis('product-123').subscribe(result => {
        expect(result.data).toEqual([]);
        expect(result.pagination?.totalCount).toBe(0);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products/product-123/apis?page=1&perPage=10`,
        method: 'GET',
      });
      req.flush(response);
    });

    it('should call the API with custom page, perPage and query', done => {
      const response = { data: [], pagination: { totalCount: 0, page: 2, perPage: 20 }, links: {} };

      apiProductV2Service.getApis('product-123', 2, 20, 'search-term').subscribe(() => done());

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products/product-123/apis?page=2&perPage=20&query=search-term`,
        method: 'GET',
      });
      req.flush(response);
    });

    it('should call the API with sortBy when provided', done => {
      const response = { data: [], pagination: { totalCount: 0, page: 1, perPage: 10 }, links: {} };

      apiProductV2Service.getApis('product-123', 1, 10, '', '-paths').subscribe(() => done());

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products/product-123/apis?page=1&perPage=10&sortBy=-paths`,
        method: 'GET',
      });
      req.flush(response);
    });
  });

  describe('update', () => {
    it('should call the API', done => {
      const apiProductId = 'product-123';
      const updateApiProduct: UpdateApiProduct = {
        name: 'Updated Name',
        version: '2.0',
        description: 'Updated description',
        apiIds: ['api-1', 'api-2', 'api-3'],
      };

      apiProductV2Service.update(apiProductId, updateApiProduct).subscribe(apiProduct => {
        expect(apiProduct.name).toEqual(updateApiProduct.name);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products/product-123`,
        method: 'PUT',
      });
      expect(req.request.body).toEqual(updateApiProduct);

      req.flush(fakeApiProduct({ ...updateApiProduct, id: apiProductId }));
    });
  });

  describe('delete', () => {
    it('should call the API', done => {
      apiProductV2Service.delete('product-123').subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products/product-123`,
        method: 'DELETE',
      });
      req.flush(null);
    });
  });

  describe('deleteApiFromApiProduct', () => {
    it('should call the API', done => {
      apiProductV2Service.deleteApiFromApiProduct('product-123', 'api-456').subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products/product-123/apis/api-456`,
        method: 'DELETE',
      });
      req.flush(null);
    });
  });

  describe('deleteAllApisFromApiProduct', () => {
    it('should call the API', done => {
      apiProductV2Service.deleteAllApisFromApiProduct('product-123').subscribe(() => {
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products/product-123/apis`,
        method: 'DELETE',
      });
      req.flush(null);
    });
  });

  describe('updateApiProductApis', () => {
    it('should call the API', done => {
      const apiIds = ['api-1', 'api-2', 'api-3'];

      apiProductV2Service.updateApiProductApis('product-123', apiIds).subscribe(apiProduct => {
        expect(apiProduct.apiIds).toEqual(apiIds);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products/product-123`,
        method: 'PUT',
      });
      expect(req.request.body).toEqual({ apiIds });

      req.flush(fakeApiProduct({ id: 'product-123', apiIds }));
    });
  });

  describe('verify', () => {
    it('should call the API with name', done => {
      apiProductV2Service.verify('My API Product').subscribe(result => {
        expect(result.ok).toBe(true);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products/_verify`,
        method: 'POST',
      });
      expect(req.request.body).toEqual({ name: 'My API Product' });

      req.flush({ ok: true });
    });

    it('should return reason when name is not unique', done => {
      apiProductV2Service.verify('Duplicate Name').subscribe(result => {
        expect(result.ok).toBe(false);
        expect(result.reason).toEqual('Name already exists');
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products/_verify`,
        method: 'POST',
      });

      req.flush({ ok: false, reason: 'Name already exists' });
    });
  });

  describe('getPermissions', () => {
    it('should GET members/permissions for the API Product', done => {
      const permissions = { MEMBER: 'CRUD', PLAN: 'R' };

      apiProductV2Service.getPermissions('product-abc').subscribe(result => {
        expect(result).toEqual(permissions);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products/product-abc/members/permissions`,
        method: 'GET',
      });
      req.flush(permissions);
    });
  });

  describe('getPagedMembers', () => {
    it('should GET members with default pagination', done => {
      const response: MembersResponse = {
        data: [{ id: 'm1', displayName: 'Alice', roles: [{ name: 'USER' }] } as Member],
        pagination: { totalCount: 1, page: 1, perPage: 10 },
      };

      apiProductV2Service.getPagedMembers('product-abc').subscribe(result => {
        expect(result.data).toHaveLength(1);
        expect(result.pagination?.totalCount).toBe(1);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products/product-abc/members?page=1&perPage=10`,
        method: 'GET',
      });
      req.flush(response);
    });

    it('should GET members with custom page and perPage', done => {
      const response: MembersResponse = { data: [], pagination: { totalCount: 0, page: 3, perPage: 25 } };

      apiProductV2Service.getPagedMembers('product-abc', 3, 25).subscribe(result => {
        expect(result.data).toEqual([]);
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products/product-abc/members?page=3&perPage=25`,
        method: 'GET',
      });
      req.flush(response);
    });
  });

  describe('addMember', () => {
    it('should POST with userId and roleName', done => {
      const membership: AddMember = { userId: 'user-1', roleName: 'USER' };
      const created: Member = { id: 'mem-1', displayName: 'Alice', roles: [{ name: 'USER' }] } as Member;

      apiProductV2Service.addMember('product-abc', membership).subscribe(member => {
        expect(member.id).toEqual('mem-1');
        done();
      });

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products/product-abc/members`,
        method: 'POST',
      });
      expect(req.request.body).toEqual(membership);
      req.flush(created);
    });

    it('should POST with externalReference when no Gravitee user id', done => {
      const membership: AddMember = { externalReference: 'idp-subject-99', roleName: 'USER' };

      apiProductV2Service.addMember('product-abc', membership).subscribe(() => done());

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products/product-abc/members`,
        method: 'POST',
      });
      expect(req.request.body).toEqual(membership);
      req.flush({ id: 'mem-ext', roles: [{ name: 'USER' }] } as Member);
    });
  });

  describe('updateMember', () => {
    it('should PUT roleName to members/{memberId}', done => {
      const membership: UpdateMember = { memberId: 'mem-99', roleName: 'OWNER' };

      apiProductV2Service.updateMember('product-abc', membership).subscribe(() => done());

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products/product-abc/members/mem-99`,
        method: 'PUT',
      });
      expect(req.request.body).toEqual({ roleName: 'OWNER' });
      req.flush({ id: 'mem-99', roles: [{ name: 'OWNER' }] } as Member);
    });
  });

  describe('deleteMember', () => {
    it('should DELETE members/{memberId}', done => {
      apiProductV2Service.deleteMember('product-abc', 'mem-1').subscribe(() => done());

      const req = httpTestingController.expectOne({
        url: `${baseURL}/api-products/product-abc/members/mem-1`,
        method: 'DELETE',
      });
      req.flush(null);
    });
  });
});
