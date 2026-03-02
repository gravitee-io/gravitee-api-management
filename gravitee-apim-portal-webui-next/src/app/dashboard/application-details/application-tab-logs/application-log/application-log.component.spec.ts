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

import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatTabGroupHarness } from '@angular/material/tabs/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { ApplicationLogComponent } from './application-log.component';
import { ConnectorsResponse, fakeConnectorsResponse } from '../../../../../entities/connector';
import {
  AggregatedMessageLogsResponse,
  fakeAggregatedMessageLog,
  fakeAggregatedMessageLogsResponse,
  Log,
  LogMetadataApi,
  fakeLog,
} from '../../../../../entities/log';
import { AppTestingModule, TESTING_BASE_URL } from '../../../../../testing/app-testing.module';
import { ApplicationLogMessagesHarness } from '../application-log-messages/application-log-messages.harness';
import { ApplicationLogRequestResponseHarness } from '../application-log-request-response/application-log-request-response.harness';

describe('ApplicationLogComponent', () => {
  let component: ApplicationLogComponent;
  let fixture: ComponentFixture<ApplicationLogComponent>;
  let httpTestingController: HttpTestingController;
  let harnessLoader: HarnessLoader;

  const APPLICATION_ID = 'application-id';
  const LOG_ID = 'log-id';
  const TIMESTAMP = '123456';

  const init = async (queryParams: unknown = { timestamp: TIMESTAMP }) => {
    await TestBed.configureTestingModule({
      imports: [ApplicationLogComponent, AppTestingModule],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { queryParams: of(queryParams) },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApplicationLogComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);

    component = fixture.componentInstance;
    component.applicationId = APPLICATION_ID;
    component.logId = LOG_ID;

    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('Missing timestamp in query params', () => {
    beforeEach(async () => {
      await init({ fakeParam: 'oops' });
    });
    it('should display error message', () => {
      expect(errorMessageDisplayed()).toEqual(true);
    });
  });

  describe('With timestamp in query params', () => {
    beforeEach(async () => {
      await init();
    });

    describe('When V2 API', () => {
      it('should only display Request and Response component', async () => {
        const metadata: LogMetadataApi = { name: 'API V2', version: 'v1.0', apiType: undefined };
        expectGetLog(
          fakeLog({
            request: { headers: { headerKey: 'value', headerKey2: 'another-value' }, body: 'Wonderful body' },
            api: 'apiV2Id',
            metadata: { apiV2Id: metadata },
          }),
        );

        expect(await harnessLoader.getHarnessOrNull(ApplicationLogRequestResponseHarness)).toBeTruthy();
        expect(await harnessLoader.getHarnessOrNull(MatTabGroupHarness)).toBeFalsy();
      });
    });

    describe('When V4 Proxy API', () => {
      it('should only display Request and Response component', async () => {
        const metadata: LogMetadataApi = { name: 'API V4 Proxy', version: 'v1.0', apiType: 'PROXY' };
        expectGetLog(
          fakeLog({
            request: { headers: { headerKey: 'value', headerKey2: 'another-value' }, body: 'Wonderful body' },
            api: 'apiV4ProxyId',
            metadata: { apiV4ProxyId: metadata },
          }),
        );

        expect(await harnessLoader.getHarnessOrNull(ApplicationLogRequestResponseHarness)).toBeTruthy();
        expect(await harnessLoader.getHarnessOrNull(MatTabGroupHarness)).toBeFalsy();
      });
    });

    describe('When V4 Message API', () => {
      it('should display Request and Response component and Message component', async () => {
        const metadata: LogMetadataApi = { name: 'API V4 Message', version: 'v1.0', apiType: 'MESSAGE' };
        const log = fakeLog({
          id: LOG_ID,
          request: { headers: { headerKey: 'value', headerKey2: 'another-value' }, body: 'Wonderful body' },
          api: 'apiV4Message',
          metadata: { apiV4Message: metadata },
        });

        expectGetLog(log);

        expect(await harnessLoader.getHarnessOrNull(ApplicationLogRequestResponseHarness)).toBeTruthy();
        const tabGroup = await harnessLoader.getHarness(MatTabGroupHarness);
        await tabGroup.selectTab({ label: 'Messages' });

        expectGetEntrypoints(fakeConnectorsResponse());
        expectGetEndpoints(fakeConnectorsResponse());
        expectGetMessageLogs(
          log.timestamp,
          fakeAggregatedMessageLogsResponse({
            data: [
              fakeAggregatedMessageLog({ correlationId: 'correlation-id-1' }),
              fakeAggregatedMessageLog({ correlationId: 'correlation-id-2' }),
            ],
            metadata: {
              data: {
                total: 2,
              },
            },
          }),
        );

        const logMessagesHarness = await harnessLoader.getHarness(ApplicationLogMessagesHarness);
        expect(await logMessagesHarness.getNumberOfRows()).toEqual(2);
      });
    });
  });

  function expectGetLog(log: Log) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${APPLICATION_ID}/logs/${LOG_ID}?timestamp=${TIMESTAMP}`).flush(log);
    fixture.detectChanges();
  }

  function errorMessageDisplayed(): boolean {
    return !!fixture.debugElement.query(By.css('[aria-label="Log error"]'));
  }

  function expectGetEntrypoints(entrypointsResponse: ConnectorsResponse) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/entrypoints`).flush(entrypointsResponse);
  }

  function expectGetEndpoints(endpointsResponse: ConnectorsResponse) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/endpoints`).flush(endpointsResponse);
  }

  function expectGetMessageLogs(timestamp: number, messageLogsResponse: AggregatedMessageLogsResponse, page: number = 1) {
    httpTestingController
      .expectOne(`${TESTING_BASE_URL}/applications/${APPLICATION_ID}/logs/${LOG_ID}/messages?page=${page}&size=10&timestamp=${timestamp}`)
      .flush(messageLogsResponse);

    fixture.detectChanges();
  }
});
