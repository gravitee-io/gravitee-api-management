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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ApplicationLogRequestResponseComponent } from './application-log-request-response.component';
import { ApplicationLogRequestResponseHarness } from './application-log-request-response.harness';
import { CopyCodeHarness } from '../../../../../components/copy-code/copy-code.harness';
import { Log, fakeLog } from '../../../../../entities/log';
import { AppTestingModule } from '../../../../../testing/app-testing.module';

describe('ApplicationLogRequestResponseComponent', () => {
  let fixture: ComponentFixture<ApplicationLogRequestResponseComponent>;
  let componentHarness: ApplicationLogRequestResponseHarness;

  const init = async (log: Log) => {
    await TestBed.configureTestingModule({
      imports: [ApplicationLogRequestResponseComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ApplicationLogRequestResponseComponent);
    fixture.componentRef.setInput('log', log);

    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApplicationLogRequestResponseHarness);
    fixture.detectChanges();
  };

  it('should display Request + Response with no headers', async () => {
    await init(fakeLog({ request: { headers: {} }, response: { headers: {} } }));

    const requestHeaders = await componentHarness.getRequestHeaders();
    expect(await requestHeaders.isExpanded()).toEqual(false);
    await requestHeaders.expand();

    expect(await requestHeaders.getTextContent()).toContain('No headers logged');

    const responseHeaders = await componentHarness.getResponseHeaders();
    expect(await responseHeaders.isExpanded()).toEqual(false);
    await responseHeaders.expand();

    expect(await responseHeaders.getTextContent()).toContain('No headers logged');
  });
  it('should display Request + Response with no body', async () => {
    await init(fakeLog({ request: { body: undefined }, response: { body: undefined } }));

    const requestBody = await componentHarness.getRequestBody();
    expect(await requestBody.isExpanded()).toEqual(false);
    await requestBody.expand();

    expect(await requestBody.getTextContent()).toContain('No content logged');

    const responseBody = await componentHarness.getResponseBody();
    expect(await responseBody.isExpanded()).toEqual(false);
    await responseBody.expand();

    expect(await responseBody.getTextContent()).toContain('No content logged');
  });
  it('should not display Request headers nor body', async () => {
    await init(fakeLog({ request: undefined }));

    const requestHeaders = await componentHarness.getRequestHeaders();
    expect(await requestHeaders.isExpanded()).toEqual(false);
    await requestHeaders.expand();

    expect(await requestHeaders.getTextContent()).toContain('No headers logged');

    const requestBody = await componentHarness.getRequestBody();
    expect(await requestBody.isExpanded()).toEqual(false);
    await requestBody.expand();

    expect(await requestBody.getTextContent()).toContain('No content logged');
  });
  it('should not display Response headers nor body', async () => {
    await init(fakeLog({ response: undefined }));

    const responseHeaders = await componentHarness.getResponseHeaders();
    expect(await responseHeaders.isExpanded()).toEqual(false);
    await responseHeaders.expand();

    expect(await responseHeaders.getTextContent()).toContain('No headers logged');

    const responseBody = await componentHarness.getResponseBody();
    expect(await responseBody.isExpanded()).toEqual(false);
    await responseBody.expand();

    expect(await responseBody.getTextContent()).toContain('No content logged');
  });
  it('should display Request headers and body', async () => {
    await init(fakeLog({ request: { headers: { headerKey: 'value', headerKey2: 'another-value' }, body: 'Wonderful body' } }));

    const requestHeaders = await componentHarness.getRequestHeaders();
    expect(await requestHeaders.isExpanded()).toEqual(false);
    await requestHeaders.expand();

    const requestHeadersTextContent = await requestHeaders.getTextContent();
    expect(requestHeadersTextContent).toContain('headerKey: value');
    expect(requestHeadersTextContent).toContain('headerKey2: another-value');

    const requestBody = await componentHarness.getRequestBody();
    expect(await requestBody.isExpanded()).toEqual(false);
    await requestBody.expand();

    const copyCode = await requestBody.getHarness(CopyCodeHarness);
    expect(await copyCode.getText()).toContain('Wonderful body');
  });
  it('should display Response headers and body', async () => {
    await init(fakeLog({ response: { headers: { headerKey: 'value', headerKey2: 'another-value' }, body: 'Wonderful body' } }));

    const responseHeaders = await componentHarness.getResponseHeaders();
    expect(await responseHeaders.isExpanded()).toEqual(false);
    await responseHeaders.expand();

    const responseHeadersTextContent = await responseHeaders.getTextContent();
    expect(responseHeadersTextContent).toContain('headerKey: value');
    expect(responseHeadersTextContent).toContain('headerKey2: another-value');

    const responseBody = await componentHarness.getResponseBody();
    expect(await responseBody.isExpanded()).toEqual(false);
    await responseBody.expand();

    const copyCode = await responseBody.getHarness(CopyCodeHarness);
    expect(await copyCode.getText()).toContain('Wonderful body');
  });
});
