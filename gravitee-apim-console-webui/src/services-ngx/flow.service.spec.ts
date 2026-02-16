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

import { FlowService } from './flow.service';

import { CONSTANTS_TESTING, GioTestingModule } from '../shared/testing';
import { fakeFlowConfigurationSchema } from '../entities/flow/configurationSchema.fixture';
import { fakePlatformFlowSchema } from '../entities/flow/platformFlowSchema.fixture';
import { fakeOrganizationFlowConfiguration } from '../entities/flow/organizationFlowConfiguration.fixture';

describe('FlowService', () => {
  let httpTestingController: HttpTestingController;
  let flowService: FlowService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioTestingModule],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    flowService = TestBed.inject<FlowService>(FlowService);
  });

  describe('getConfigurationSchemaForm', () => {
    it('should call the API', done => {
      const flowConfigurationSchema = fakeFlowConfigurationSchema();

      flowService.getConfigurationSchemaForm().subscribe(response => {
        expect(response).toStrictEqual(flowConfigurationSchema);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.org.baseURL}/configuration/flows/configuration-schema`,
        })
        .flush(flowConfigurationSchema);
    });
  });

  describe('getPlatformFlowSchemaForm', () => {
    it('should call the API', done => {
      const platformFlowSchema = fakePlatformFlowSchema();

      flowService.getPlatformFlowSchemaForm().subscribe(response => {
        expect(response).toStrictEqual(platformFlowSchema);
        done();
      });

      httpTestingController
        .expectOne({
          method: 'GET',
          url: `${CONSTANTS_TESTING.org.baseURL}/configuration/flows/flow-schema`,
        })
        .flush(platformFlowSchema);
    });
  });

  describe('getConfiguration', () => {
    it('should call the API', done => {
      const platformFlowSchema = fakeOrganizationFlowConfiguration();

      flowService.getConfiguration().subscribe(response => {
        expect(response).toStrictEqual(platformFlowSchema);
        done();
      });

      httpTestingController
        .expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.org.baseURL}/configuration/flows` })
        .flush(platformFlowSchema);
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
