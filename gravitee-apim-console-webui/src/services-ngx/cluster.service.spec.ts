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

import { ClusterService } from './cluster.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import {
  Cluster,
  CreateCluster,
  fakeCluster,
  fakeCreateCluster,
  fakePagedResult,
  fakeUpdateCluster,
  PagedResult,
  UpdateCluster,
} from '../entities/management-api-v2';

describe('ClusterService', () => {
  let httpTestingController: HttpTestingController;
  let service: ClusterService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject<ClusterService>(ClusterService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('get', () => {
    it('should call the API', done => {
      service.get(fakeCluster().id).subscribe(cluster => {
        expect(cluster).toBeTruthy();
        done();
      });

      expectGetClusterRequest(httpTestingController, fakeCluster());
    });
  });

  describe('create', () => {
    it('should call the API', done => {
      service.create(fakeCreateCluster()).subscribe(cluster => {
        expect(cluster).toBeTruthy();
        done();
      });

      expectCreateClusterRequest(httpTestingController, fakeCreateCluster());
    });
  });

  describe('update', () => {
    it('should call the API', done => {
      service.update('clusterId', fakeUpdateCluster()).subscribe(cluster => {
        expect(cluster).toBeTruthy();
        done();
      });

      expectUpdateClusterRequest(httpTestingController, 'clusterId', fakeUpdateCluster());
    });
  });

  describe('updateGroups', () => {
    it('should call the API', done => {
      const groupId = ['group1', 'group2'];
      service.updateGroups('clusterId', groupId).subscribe(cluster => {
        expect(cluster).toBeTruthy();
        done();
      });

      expectUpdateGroupsRequest(httpTestingController, 'clusterId', groupId);
    });
  });

  describe('delete', () => {
    it('should call the API', done => {
      service.delete('clusterId').subscribe(() => {
        done();
      });
      expectDeleteClusterRequest(httpTestingController, 'clusterId');
    });
  });

  describe('list', () => {
    it('should call the API', done => {
      service.list().subscribe(clusters => {
        expect(clusters.data.length).toEqual(1);
        done();
      });

      expectListClusterRequest(httpTestingController, fakePagedResult([fakeCluster()]));
    });
  });

  describe('getConfigurationSchema', () => {
    it('should call the API', done => {
      service.getConfigurationSchema().subscribe(schema => {
        expect(schema).toBeTruthy();
        done();
      });

      expectGetConfigurationSchemaRequest(httpTestingController);
    });
  });

  describe('getPermissions', () => {
    it('should call the API', done => {
      const cluster = fakeCluster();
      service.getPermissions(cluster.id).subscribe(permissions => {
        expect(permissions).toBeTruthy();
        done();
      });

      expectGetClusterPermissionsRequest(httpTestingController, cluster.id, { 'cluster-member': 'R' });
    });
  });
});

export const expectListClusterRequest = (
  httpTestingController: HttpTestingController,
  clusters: PagedResult<Cluster> = fakePagedResult([fakeCluster()]),
  queryParams: string = '?page=1&perPage=25',
) => {
  const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/clusters${queryParams}`);
  expect(req.request.method).toEqual('GET');
  req.flush(clusters);
};

export const expectGetClusterRequest = (httpTestingController: HttpTestingController, cluster: Cluster = fakeCluster()) => {
  const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/clusters/${cluster.id}`);
  expect(req.request.method).toEqual('GET');
  req.flush(cluster);
};

export const expectCreateClusterRequest = (
  httpTestingController: HttpTestingController,
  expectedCreateCluster: CreateCluster,
  clusterCreated: Cluster = fakeCluster(),
) => {
  const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/clusters`);
  expect(req.request.method).toEqual('POST');
  expect(req.request.body).toStrictEqual(expectedCreateCluster);
  req.flush(clusterCreated);
};

export const expectUpdateClusterRequest = (
  httpTestingController: HttpTestingController,
  clusterId: string,
  expectedUpdateCluster: UpdateCluster,
  clusterUpdated: Cluster = fakeCluster(),
) => {
  const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/clusters/${clusterId}`);
  expect(req.request.method).toEqual('PUT');
  expect(req.request.body).toStrictEqual(expectedUpdateCluster);
  req.flush(clusterUpdated);
};

export const expectDeleteClusterRequest = (httpTestingController: HttpTestingController, clusterId: string) => {
  const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/clusters/${clusterId}`);
  expect(req.request.method).toEqual('DELETE');
  req.flush({});
};

export const expectUpdateGroupsRequest = (
  httpTestingController: HttpTestingController,
  clusterId: string,
  groups: string[],
  clusterUpdated: Cluster = fakeCluster(),
) => {
  const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/clusters/${clusterId}/groups`);
  expect(req.request.method).toEqual('PUT');
  expect(req.request.body).toStrictEqual(groups);
  req.flush(clusterUpdated);
};

export const expectGetClusterPermissionsRequest = (
  httpTestingController: HttpTestingController,
  clusterId: string,
  permissions: Record<string, string> = { 'cluster-member': 'R' },
) => {
  const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/clusters/${clusterId}/permissions`);
  expect(req.request.method).toEqual('GET');
  req.flush(permissions);
};

export const expectGetConfigurationSchemaRequest = (httpTestingController: HttpTestingController, schema: unknown = {}) => {
  const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/clusters/schema/configuration`);
  expect(req.request.method).toEqual('GET');
  req.flush(schema);
};
