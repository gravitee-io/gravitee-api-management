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

import { ApplicationLogMessagesComponent } from './application-log-messages.component';
import { ApplicationLogMessagesHarness } from './application-log-messages.harness';
import { ConnectorsResponse, fakeConnectorsResponse } from '../../../../../entities/connector';
import {
  fakeAggregatedMessageLog,
  fakeLog,
  fakeAggregatedMessageLogsResponse,
  AggregatedMessageLogsResponse,
} from '../../../../../entities/log';
import { AppTestingModule, TESTING_BASE_URL } from '../../../../../testing/app-testing.module';
import { MessageLogDetailDialogHarness } from '../message-log-detail-dialog/message-log-detail-dialog.harness';

describe('ApplicationLogMessagesComponent', () => {
  const APP_ID = 'app-id';
  const LOG_ID = 'log-id';
  const LOG = fakeLog({ id: LOG_ID });

  let fixture: ComponentFixture<ApplicationLogMessagesComponent>;
  let rootHarnessLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let componentHarness: ApplicationLogMessagesHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationLogMessagesComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ApplicationLogMessagesComponent);
    httpTestingController = TestBed.inject(HttpTestingController);

    rootHarnessLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApplicationLogMessagesHarness);

    fixture.componentRef.setInput('applicationId', APP_ID);
    fixture.componentRef.setInput('log', LOG);

    fixture.detectChanges();

    expectGetEntrypoints(fakeConnectorsResponse({ data: [] }));
    expectGetEndpoints(fakeConnectorsResponse({ data: [] }));
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should show message when no results', async () => {
    expectGetMessageLogs(fakeAggregatedMessageLogsResponse({ data: [] }));
    expect(await componentHarness.isNoLogsMessageShown()).toEqual(true);
  });

  it('should show error message on error', async () => {
    expectGetMessageLogsError();
    expect(await componentHarness.isErrorMessageShown()).toEqual(true);
  });

  it('should show page of results', async () => {
    expectGetMessageLogs(
      fakeAggregatedMessageLogsResponse({
        data: [
          fakeAggregatedMessageLog({ correlationId: 'correlation-id-1' }),
          fakeAggregatedMessageLog({ correlationId: 'correlation-id-2' }),
        ],
        metadata: {
          data: { total: 2 },
        },
      }),
    );

    expect(await componentHarness.getNumberOfRows()).toEqual(2);
    expect(await componentHarness.getCorrelationIdByRow(0)).toEqual('correlation-id-1');
    expect(await componentHarness.getCorrelationIdByRow(1)).toEqual('correlation-id-2');
  });

  it('should go to new page of results on pagination', async () => {
    expectGetMessageLogs(
      fakeAggregatedMessageLogsResponse({
        data: [
          fakeAggregatedMessageLog({ correlationId: 'correlation-id-1' }),
          fakeAggregatedMessageLog({ correlationId: 'correlation-id-2' }),
          fakeAggregatedMessageLog({ correlationId: 'correlation-id-3' }),
          fakeAggregatedMessageLog({ correlationId: 'correlation-id-4' }),
          fakeAggregatedMessageLog({ correlationId: 'correlation-id-5' }),
          fakeAggregatedMessageLog({ correlationId: 'correlation-id-6' }),
          fakeAggregatedMessageLog({ correlationId: 'correlation-id-7' }),
          fakeAggregatedMessageLog({ correlationId: 'correlation-id-8' }),
          fakeAggregatedMessageLog({ correlationId: 'correlation-id-9' }),
          fakeAggregatedMessageLog({ correlationId: 'correlation-id-10' }),
        ],
        metadata: {
          data: { total: 15 },
        },
      }),
    );

    expect(await componentHarness.getNumberOfRows()).toEqual(10);
    expect(await componentHarness.getCorrelationIdByRow(0)).toEqual('correlation-id-1');

    const pagination = await componentHarness.getPagination();
    const currentPage = await pagination.getCurrentPaginationPage();
    expect(await currentPage.getText()).toEqual('1');

    expect(await pagination.getPageButtonByNumber(2).then(btn => btn.isDisabled())).toEqual(false);
    expect(await pagination.getPreviousPageButton().then(btn => btn.isDisabled())).toEqual(true);
    await pagination.getNextPageButton().then(btn => btn.click());

    expectGetMessageLogs(fakeAggregatedMessageLogsResponse({ data: [fakeAggregatedMessageLog()] }), 2);
  });

  it('should open application message logs dialog', async () => {
    expectGetMessageLogs(
      fakeAggregatedMessageLogsResponse({
        data: [
          fakeAggregatedMessageLog({ correlationId: 'correlation-id-1' }),
          fakeAggregatedMessageLog({ correlationId: 'correlation-id-2' }),
        ],
        metadata: {
          data: { total: 2 },
        },
      }),
    );

    await componentHarness.clickOnRowByRowIndex(0);

    const messageLogDialog = await rootHarnessLoader.getHarness(MessageLogDetailDialogHarness);
    expect(await messageLogDialog.isEntrypointCardShown()).toEqual(true);
    expect(await messageLogDialog.isEndpointCardShown()).toEqual(true);
  });

  function expectGetMessageLogs(messageLogsResponse: AggregatedMessageLogsResponse, page: number = 1) {
    httpTestingController
      .expectOne(`${TESTING_BASE_URL}/applications/${APP_ID}/logs/${LOG_ID}/messages?page=${page}&size=10&timestamp=${LOG.timestamp}`)
      .flush(messageLogsResponse);

    fixture.detectChanges();
  }

  function expectGetMessageLogsError() {
    httpTestingController
      .expectOne(`${TESTING_BASE_URL}/applications/${APP_ID}/logs/${LOG_ID}/messages?page=1&size=10&timestamp=${LOG.timestamp}`)
      .flush({ error: { message: 'No good!' } }, { status: 403, statusText: 'Permission Denied' });

    fixture.detectChanges();
  }

  function expectGetEntrypoints(entrypointsResponse: ConnectorsResponse) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/entrypoints`).flush(entrypointsResponse);
  }

  function expectGetEndpoints(endpointsResponse: ConnectorsResponse) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/endpoints`).flush(endpointsResponse);
  }
});
