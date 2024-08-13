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
import { MatExpansionPanelHarness } from '@angular/material/expansion/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { ApplicationLogComponent } from './application-log.component';
import { CopyCodeHarness } from '../../../../../components/copy-code/copy-code.harness';
import { Log } from '../../../../../entities/log/log';
import { fakeLog } from '../../../../../entities/log/log.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../../../../../testing/app-testing.module';

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
    it('should display Request + Response with no headers', async () => {
      expectGetLog(fakeLog({ request: { headers: {} }, response: { headers: {} } }));
      const requestHeaders = await getRequestHeaders();
      expect(await requestHeaders.isExpanded()).toEqual(false);
      await requestHeaders.expand();

      expect(await requestHeaders.getTextContent()).toContain('No headers logged');

      const responseHeaders = await getResponseHeaders();
      expect(await responseHeaders.isExpanded()).toEqual(false);
      await responseHeaders.expand();

      expect(await responseHeaders.getTextContent()).toContain('No headers logged');
    });
    it('should display Request + Response with no body', async () => {
      expectGetLog(fakeLog({ request: { body: undefined }, response: { body: undefined } }));
      const requestBody = await getRequestBody();
      expect(await requestBody.isExpanded()).toEqual(false);
      await requestBody.expand();

      expect(await requestBody.getTextContent()).toContain('No content logged');

      const responseBody = await getResponseBody();
      expect(await responseBody.isExpanded()).toEqual(false);
      await responseBody.expand();

      expect(await responseBody.getTextContent()).toContain('No content logged');
    });
    it('should not display Request headers nor body', async () => {
      expectGetLog(fakeLog({ request: undefined }));
      const requestHeaders = await getRequestHeaders();
      expect(await requestHeaders.isExpanded()).toEqual(false);
      await requestHeaders.expand();

      expect(await requestHeaders.getTextContent()).toContain('No headers logged');

      const requestBody = await getRequestBody();
      expect(await requestBody.isExpanded()).toEqual(false);
      await requestBody.expand();

      expect(await requestBody.getTextContent()).toContain('No content logged');
    });
    it('should not display Response headers nor body', async () => {
      expectGetLog(fakeLog({ response: undefined }));

      const responseHeaders = await getResponseHeaders();
      expect(await responseHeaders.isExpanded()).toEqual(false);
      await responseHeaders.expand();

      expect(await responseHeaders.getTextContent()).toContain('No headers logged');

      const responseBody = await getResponseBody();
      expect(await responseBody.isExpanded()).toEqual(false);
      await responseBody.expand();

      expect(await responseBody.getTextContent()).toContain('No content logged');
    });
    it('should display Request headers and body', async () => {
      expectGetLog(fakeLog({ request: { headers: { headerKey: 'value', headerKey2: 'another-value' }, body: 'Wonderful body' } }));
      const requestHeaders = await getRequestHeaders();
      expect(await requestHeaders.isExpanded()).toEqual(false);
      await requestHeaders.expand();

      const requestHeadersTextContent = await requestHeaders.getTextContent();
      expect(requestHeadersTextContent).toContain('headerKey: value');
      expect(requestHeadersTextContent).toContain('headerKey2: another-value');

      const requestBody = await getRequestBody();
      expect(await requestBody.isExpanded()).toEqual(false);
      await requestBody.expand();

      const copyCode = await requestBody.getHarness(CopyCodeHarness);
      expect(await copyCode.getText()).toContain('Wonderful body');
    });
    it('should display Response headers and body', async () => {
      expectGetLog(fakeLog({ response: { headers: { headerKey: 'value', headerKey2: 'another-value' }, body: 'Wonderful body' } }));
      const responseHeaders = await getResponseHeaders();
      expect(await responseHeaders.isExpanded()).toEqual(false);
      await responseHeaders.expand();

      const responseHeadersTextContent = await responseHeaders.getTextContent();
      expect(responseHeadersTextContent).toContain('headerKey: value');
      expect(responseHeadersTextContent).toContain('headerKey2: another-value');

      const responseBody = await getResponseBody();
      expect(await responseBody.isExpanded()).toEqual(false);
      await responseBody.expand();

      const copyCode = await responseBody.getHarness(CopyCodeHarness);
      expect(await copyCode.getText()).toContain('Wonderful body');
    });
  });

  function expectGetLog(log: Log) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/applications/${APPLICATION_ID}/logs/${LOG_ID}?timestamp=${TIMESTAMP}`).flush(log);
    fixture.detectChanges();
  }

  function errorMessageDisplayed(): boolean {
    return !!fixture.debugElement.query(By.css('[aria-label="Log error"]'));
  }

  async function getRequestHeaders(): Promise<MatExpansionPanelHarness> {
    return await getExpansionPanelByAriaLabel('Request Headers');
  }

  async function getRequestBody(): Promise<MatExpansionPanelHarness> {
    return await getExpansionPanelByAriaLabel('Request Body');
  }

  async function getResponseHeaders(): Promise<MatExpansionPanelHarness> {
    return await getExpansionPanelByAriaLabel('Response Headers');
  }

  async function getResponseBody(): Promise<MatExpansionPanelHarness> {
    return await getExpansionPanelByAriaLabel('Response Body');
  }

  async function getExpansionPanelByAriaLabel(value: string): Promise<MatExpansionPanelHarness> {
    return harnessLoader.getHarness(MatExpansionPanelHarness.with({ selector: `[aria-label="${value}"]` }));
  }
});
