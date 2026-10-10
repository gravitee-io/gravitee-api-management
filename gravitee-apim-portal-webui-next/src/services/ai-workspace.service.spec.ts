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

import { AiWorkspaceService } from './ai-workspace.service';
import { AppTestingModule, TESTING_BASE_URL } from '../testing/app-testing.module';

describe('AiWorkspaceService', () => {
  let service: AiWorkspaceService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppTestingModule],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    service = TestBed.inject(AiWorkspaceService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('lists a page without a name filter when the query is blank', () => {
    service.list(2, 8, '   ').subscribe();

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/ai-workspaces?page=2&size=8`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.has('name')).toBe(false);
    req.flush({ data: [] });
  });

  it('lists a page filtered by name', () => {
    service.list(1, 20, ' spf ').subscribe();

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/ai-workspaces?page=1&size=20&name=spf`);
    req.flush({ data: [] });
  });

  it('loads one workspace', () => {
    service.get('ws-1').subscribe(workspace => {
      expect(workspace.name).toBe('test-local-spf');
    });

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/ai-workspaces/ws-1`);
    req.flush({ id: 'ws-1', name: 'test-local-spf' });
  });

  it('returns zeros when consumption fails', () => {
    service.getConsumption('ws-1').subscribe(consumption => {
      expect(consumption).toEqual({ tokens: 0, requests: 0, cost: 0, from: '', to: '' });
    });

    httpTestingController
      .expectOne(`${TESTING_BASE_URL}/ai-workspaces/ws-1/consumption`)
      .flush('nope', { status: 500, statusText: 'Error' });
  });
});
