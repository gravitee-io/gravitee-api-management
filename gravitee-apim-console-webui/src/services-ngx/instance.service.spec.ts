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

import { InstanceService } from './instance.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeInstance, fakeMonitoringData } from '../entities/instance/instance.fixture';
import { fakeSearchResult } from '../entities/instance/instanceListItem.fixture';

describe('InstanceService', () => {
  let httpTestingController: HttpTestingController;
  let instanceService: InstanceService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    instanceService = TestBed.inject<InstanceService>(InstanceService);
  });

  describe('get', () => {
    it('should call the API', done => {
      const instanceId = '5bc17c57-b350-460d-817c-57b350060db3';
      const fakeInstanceObject = fakeInstance();

      instanceService.get(instanceId).subscribe(instance => {
        expect(instance).toMatchObject(fakeInstanceObject);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/instances/${instanceId}`);
      expect(req.request.method).toEqual('GET');

      req.flush(fakeInstanceObject);
    });
  });

  describe('getMonitoringData', () => {
    it('should call the API', done => {
      const instanceId = '5bc17c57-b350-460d-817c-57b350060db3';
      const monitoringId = '74bf7316-6475-416f-bf73-166475816fc5';
      const fakeMonitoringDataObject = fakeMonitoringData();

      instanceService.getMonitoringData(instanceId, monitoringId).subscribe(monitoringData => {
        expect(monitoringData).toMatchObject(fakeMonitoringDataObject);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/instances/${instanceId}/monitoring/${monitoringId}`);
      expect(req.request.method).toEqual('GET');

      req.flush(fakeMonitoringDataObject);
    });
  });

  describe('search', () => {
    it('should call the API', done => {
      const includeStopped = false;
      const from = 0;
      const to = 0;
      const page = 0;
      const size = 100;
      const fakeSearchResults = [fakeSearchResult()];

      instanceService.search(includeStopped, from, to, page, size).subscribe(searchResults => {
        expect(searchResults).toMatchObject(fakeSearchResults);
        done();
      });

      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.env.baseURL}/instances/?includeStopped=${includeStopped}&from=${from}&to=${to}&page=${page}&size=${size}`,
      );
      expect(req.request.method).toEqual('GET');

      req.flush(fakeSearchResults);
    });
  });

  describe('getByGatewayId', () => {
    it('should call the API', done => {
      const instanceId = '5bc17c57-b350-460d-817c-57b350060db3';
      const fakeInstanceObject = fakeInstance();

      instanceService.getByGatewayId(instanceId).subscribe(instance => {
        expect(instance).toMatchObject(fakeInstanceObject);
        done();
      });

      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/instances/${instanceId}`);
      expect(req.request.method).toEqual('GET');

      req.flush(fakeInstanceObject);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
