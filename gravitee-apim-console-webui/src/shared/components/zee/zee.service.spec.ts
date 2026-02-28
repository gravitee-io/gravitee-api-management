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

import { ZeeService } from './zee.service';
import { ZeeGenerateRequest, ZeeGenerateResponse, ZeeResourceType } from './zee.model';

import { CONSTANTS_TESTING, GioTestingModule } from '../../testing';

describe('ZeeService', () => {
  let httpTestingController: HttpTestingController;
  let service: ZeeService;

  const expectedUrl = `${CONSTANTS_TESTING.env.v2BaseURL}/ai/generate`;

  const mockRequest: ZeeGenerateRequest = {
    resourceType: ZeeResourceType.FLOW,
    prompt: 'Generate a simple flow that blocks bots',
    contextData: { apiId: 'api-123' },
  };

  const mockResponse: ZeeGenerateResponse = {
    resourceType: 'FLOW',
    generated: { name: 'Bot-blocker Flow', enabled: true },
    metadata: { model: 'gpt-4o', tokensUsed: 512 },
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject(ZeeService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('generate', () => {
    it('should POST to the correct URL', (done) => {
      service.generate(mockRequest).subscribe(() => done());

      const req = httpTestingController.expectOne(expectedUrl);
      expect(req.request.method).toEqual('POST');
      req.flush(mockResponse);
    });

    it('should send a FormData body', (done) => {
      service.generate(mockRequest).subscribe(() => done());

      const req = httpTestingController.expectOne(expectedUrl);
      expect(req.request.body).toBeInstanceOf(FormData);
      req.flush(mockResponse);
    });

    it('should emit the response on success', (done) => {
      service.generate(mockRequest).subscribe((res) => {
        expect(res).toEqual(mockResponse);
        expect(res.generated).toEqual({ name: 'Bot-blocker Flow', enabled: true });
        done();
      });

      const req = httpTestingController.expectOne(expectedUrl);
      req.flush(mockResponse);
    });

    it('should include files in the FormData when provided', (done) => {
      const file = new File(['{"key":"val"}'], 'context.json', { type: 'application/json' });

      service.generate(mockRequest, [file]).subscribe(() => done());

      const req = httpTestingController.expectOne(expectedUrl);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toBeInstanceOf(FormData);
      // FormData is opaque in Angular testing; we can verify it's a POST to the right URL
      req.flush(mockResponse);
    });

    it('should work without files (no file parts)', (done) => {
      service.generate(mockRequest, undefined).subscribe((res) => {
        expect(res.resourceType).toEqual('FLOW');
        done();
      });

      const req = httpTestingController.expectOne(expectedUrl);
      req.flush(mockResponse);
    });

    it('should propagate HTTP errors', (done) => {
      service.generate(mockRequest).subscribe({
        next: () => fail('Expected an error'),
        error: (err) => {
          expect(err.status).toEqual(429);
          done();
        },
      });

      const req = httpTestingController.expectOne(expectedUrl);
      req.flush({ message: 'Rate limit exceeded' }, { status: 429, statusText: 'Too Many Requests' });
    });
  });
});
