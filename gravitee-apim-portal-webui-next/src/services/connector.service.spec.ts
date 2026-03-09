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

import { ConnectorService } from './connector.service';
import { fakeConnectorsResponse } from '../entities/connector';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('ConnectorService', () => {
  let service: ConnectorService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [AppTestingModule] });
    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject(ConnectorService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should return entrypoints', done => {
    const connectorsResponse = fakeConnectorsResponse();

    service.getEntrypoints().subscribe(response => {
      expect(response).toMatchObject(connectorsResponse);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/entrypoints`);
    expect(req.request.method).toEqual('GET');

    req.flush(connectorsResponse);
  });

  it('should return endpoints', done => {
    const connectorsResponse = fakeConnectorsResponse();

    service.getEndpoints().subscribe(response => {
      expect(response).toMatchObject(connectorsResponse);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/endpoints`);
    expect(req.request.method).toEqual('GET');

    req.flush(connectorsResponse);
  });
});
