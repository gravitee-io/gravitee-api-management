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

import { InteractivityChecker } from '@angular/cdk/a11y';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { PolicyStudioDebugComponent } from './policy-studio-debug.component';
import { PolicyStudioDebugModule } from './policy-studio-debug.module';

import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { fakeApi } from '../../../../entities/api/Api.fixture';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { fakePolicyListItem } from '../../../../entities/policy';

describe('PolicyStudioDebugComponent - Try It', () => {
  let fixture: ComponentFixture<PolicyStudioDebugComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;

  const policies = [fakePolicyListItem()];
  const api = fakeApi();

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, PolicyStudioDebugModule],
      providers: [{ provide: UIRouterStateParams, useValue: { apiId: api.id, tryItDisplay: true } }],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(PolicyStudioDebugComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/policies?expand=icon&withResource=false`).flush(policies);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should send simple debug request', fakeAsync(async () => {
    const eventId = 'eventId';

    const sendButton = await loader.getHarness(MatButtonHarness.with({ text: 'Send' }));

    await sendButton.click();

    tick();

    expectSendDebugEvent(eventId);
    tick(1000);
    expectGetDebugEvent(eventId, false);
    tick(1000);
    expectGetDebugEvent(eventId, true);

    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.policy-studio-debug-response__display-response__header').textContent).toContain(
      'Response   200 - OK   GET /',
    );

    expect(fixture.nativeElement.querySelector('.policy-studio-debug-response__display-response__content-headers').textContent).toContain(
      'Content-Type: text/plain',
    );
  }));

  function expectSendDebugEvent(eventId: string) {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}`).flush(api);

    httpTestingController
      .expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}/_debug`,
      })
      .flush({ id: eventId });
  }

  function expectGetDebugEvent(eventId: string, success: boolean) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}/events/${eventId}`,
      })
      .flush({
        type: 'DEBUG_API',
        properties: {
          api_debug_status: success ? 'SUCCESS' : 'FAILED',
        },
        payload: JSON.stringify({
          response: {
            statusCode: 200,
            body: 'Ok',
            headers: {
              'Content-Type': ['text/plain'],
            },
          },
          request: {
            path: '/',
            method: 'GET',
          },
        }),
      });
  }
});
