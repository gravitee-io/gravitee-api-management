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
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { AgentCatalogService } from './agent-catalog.service';
import { ConfigService } from './config.service';
import { TESTING_BASE_URL } from '../testing/app-testing.module';

describe('AgentCatalogService', () => {
  let service: AgentCatalogService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ConfigService, useValue: { baseURL: TESTING_BASE_URL } },
        AgentCatalogService,
      ],
    });

    service = TestBed.inject(AgentCatalogService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('searchAgents', () => {
    it('should call the Gamma catalog API with search query', () => {
      const mockResponse = {
        data: [
          {
            id: 'agent-1',
            kind: 'agent',
            definition: { name: 'Test Agent', version: '1.0.0', capabilities: {}, skills: [], defaultInputModes: [], defaultOutputModes: [] },
          },
        ],
        pagination: { page: 1, perPage: 5, pageCount: 1, totalCount: 1 },
      };

      service.searchAgents('DEFAULT', 'DEFAULT', 'Test Agent').subscribe(result => {
        expect(result.data).toHaveLength(1);
        expect(result.data[0].definition.name).toBe('Test Agent');
      });

      const req = httpMock.expectOne(
        r => r.url.includes('/gamma/organizations/DEFAULT/environments/DEFAULT/modules/aim/catalog/agents') && r.params.get('q') === 'Test Agent',
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });
  });

  describe('getAgentById', () => {
    it('should fetch a single agent by ID', () => {
      const mockAgent = {
        id: 'agent-1',
        kind: 'agent',
        definition: { name: 'Test Agent', version: '1.0.0', capabilities: {}, skills: [], defaultInputModes: [], defaultOutputModes: [] },
      };

      service.getAgentById('DEFAULT', 'DEFAULT', 'agent-1').subscribe(result => {
        expect(result.id).toBe('agent-1');
      });

      const req = httpMock.expectOne(r => r.url.endsWith('/agents/agent-1'));
      expect(req.request.method).toBe('GET');
      req.flush(mockAgent);
    });
  });

  describe('findAgentByName', () => {
    it('should return the agent matching the exact name', () => {
      const mockResponse = {
        data: [
          {
            id: 'agent-other',
            kind: 'agent',
            definition: { name: 'Other Agent', version: '1.0.0', capabilities: {}, skills: [], defaultInputModes: [], defaultOutputModes: [] },
          },
          {
            id: 'agent-1',
            kind: 'agent',
            definition: { name: 'Test Agent', version: '1.0.0', capabilities: {}, skills: [], defaultInputModes: [], defaultOutputModes: [] },
          },
        ],
        pagination: { page: 1, perPage: 5, pageCount: 1, totalCount: 2 },
      };

      service.findAgentByName('DEFAULT', 'DEFAULT', 'Test Agent').subscribe(result => {
        expect(result).not.toBeNull();
        expect(result!.id).toBe('agent-1');
      });

      const req = httpMock.expectOne(r => r.url.includes('/agents') && r.params.get('q') === 'Test Agent');
      req.flush(mockResponse);
    });

    it('should return the first result when no exact name match', () => {
      const mockResponse = {
        data: [
          {
            id: 'agent-fuzzy',
            kind: 'agent',
            definition: { name: 'Fuzzy Match', version: '1.0.0', capabilities: {}, skills: [], defaultInputModes: [], defaultOutputModes: [] },
          },
        ],
        pagination: { page: 1, perPage: 5, pageCount: 1, totalCount: 1 },
      };

      service.findAgentByName('DEFAULT', 'DEFAULT', 'Not Found').subscribe(result => {
        expect(result).not.toBeNull();
        expect(result!.id).toBe('agent-fuzzy');
      });

      const req = httpMock.expectOne(r => r.url.includes('/agents') && r.params.get('q') === 'Not Found');
      req.flush(mockResponse);
    });

    it('should return null when no results', () => {
      const mockResponse = {
        data: [],
        pagination: { page: 1, perPage: 5, pageCount: 0, totalCount: 0 },
      };

      service.findAgentByName('DEFAULT', 'DEFAULT', 'Empty').subscribe(result => {
        expect(result).toBeNull();
      });

      const req = httpMock.expectOne(r => r.url.includes('/agents') && r.params.get('q') === 'Empty');
      req.flush(mockResponse);
    });
  });
});
