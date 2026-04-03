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

import { ApplicationCertificateService } from './application-certificate.service';
import { fakeClientCertificate } from '../entities/application/client-certificate.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('ApplicationCertificateService', () => {
  let service: ApplicationCertificateService;
  let httpTestingController: HttpTestingController;
  const appId = 'app-1';
  const certId = 'cert-1';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    service = TestBed.inject(ApplicationCertificateService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should GET application certificates with pagination', done => {
    const response = { data: [fakeClientCertificate()], metadata: { paginateMetaData: { totalElements: 1 } } };

    service.list(appId, 1, 10).subscribe(res => {
      expect(res).toMatchObject(response);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${appId}/certificates?page=1&size=10`);
    expect(req.request.method).toEqual('GET');
    req.flush(response);
  });

  it('should POST a new application certificate', done => {
    const input = { name: 'New Cert', certificate: '-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----' };
    const created = fakeClientCertificate({ id: 'cert-new', name: 'New Cert' });

    service.create(appId, input).subscribe(res => {
      expect(res).toMatchObject(created);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${appId}/certificates`);
    expect(req.request.method).toEqual('POST');
    expect(req.request.body).toEqual(input);
    req.flush(created);
  });

  it('should PUT updated application certificate', done => {
    const input = { name: 'Updated Cert' };
    const updated = fakeClientCertificate({ id: certId, name: 'Updated Cert' });

    service.update(appId, certId, input).subscribe(res => {
      expect(res).toMatchObject(updated);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${appId}/certificates/${certId}`);
    expect(req.request.method).toEqual('PUT');
    expect(req.request.body).toEqual(input);
    req.flush(updated);
  });

  it('should DELETE an application certificate', done => {
    service.delete(appId, certId).subscribe(() => {
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${appId}/certificates/${certId}`);
    expect(req.request.method).toEqual('DELETE');
    req.flush(null);
  });

  it('should POST to _validate and return certificate metadata', done => {
    const certificate = '-----BEGIN CERTIFICATE-----\nMIIBkTCB+wIJAKHBfpE...\n-----END CERTIFICATE-----';
    const response = { certificateExpiration: '2027-06-01T00:00:00.000Z', subject: 'CN=test', issuer: 'CN=ca' };

    service.validate(appId, certificate).subscribe(res => {
      expect(res).toMatchObject(response);
      done();
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${appId}/certificates/_validate`);
    expect(req.request.method).toEqual('POST');
    expect(req.request.body).toEqual({ certificate });
    req.flush(response);
  });
});
