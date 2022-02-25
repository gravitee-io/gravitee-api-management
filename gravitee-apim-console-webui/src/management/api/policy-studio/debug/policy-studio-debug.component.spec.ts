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
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { PolicyStudioDebugComponent } from './policy-studio-debug.component';
import { PolicyStudioDebugModule } from './policy-studio-debug.module';
import { fakeDebugEvent } from './models/DebugEvent.fixture';

import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';
import { fakeApi } from '../../../../entities/api/Api.fixture';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../../shared/testing';
import { fakePolicyListItem } from '../../../../entities/policy';
import { PolicyStudioService } from '../policy-studio.service';
import { toApiDefinition } from '../models/ApiDefinition';

describe('PolicyStudioDebugComponent', () => {
  let fixture: ComponentFixture<PolicyStudioDebugComponent>;
  let httpTestingController: HttpTestingController;
  let policyStudioService: PolicyStudioService;
  let loader: HarnessLoader;

  const policies = [fakePolicyListItem()];
  const api = fakeApi();

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, PolicyStudioDebugModule, MatIconTestingModule],
      providers: [{ provide: UIRouterStateParams, useValue: { apiId: api.id } }],
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

    policyStudioService = TestBed.inject(PolicyStudioService);
    policyStudioService.emitApiDefinition(toApiDefinition(api));
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
      'Response 200 - OK  GET /',
    );
  }));

  describe('when debug response is success', () => {
    const EVENT_ID = 'eventId';

    beforeEach(fakeAsync(async () => {
      const sendButton = await loader.getHarness(MatButtonHarness.with({ text: 'Send' }));
      await sendButton.click();

      tick();
      expectSendDebugEvent(EVENT_ID);
      tick(1000);
      expectGetDebugEvent(EVENT_ID, true);

      fixture.detectChanges();
    }));

    it('should display timeline', () => {
      const timeLineCards = [...fixture.nativeElement.querySelectorAll('.policy-studio-debug-timeline-card').values()].map((card) => ({
        content: card.querySelector('.policy-studio-debug-timeline-card__content').textContent,
        state: card.querySelector('.policy-studio-debug-timeline-card__right').innerHTML.includes('gio:warning-circled-outline')
          ? 'ERROR'
          : card.querySelector('.policy-studio-debug-timeline-card__right').innerHTML.includes('gio:warning-circled-outline')
          ? 'SKIPPED'
          : undefined,
      }));
      expect(timeLineCards).toEqual([
        {
          content: ' Client APP ',
        },
        {
          content: ' Request Input ',
        },
        {
          content: ' Header  key-less ',
        },
        {
          content: ' Header  policy-assign-attributes ',
        },
        {
          content: ' Header  policy-override-request-method ',
        },
        {
          content: ' Header  transform-headers ',
        },
        {
          content: ' Header  transform-headers ',
          state: 'ERROR',
        },
        {
          content: ' Request Output ',
        },
        {
          content: ' Backend Target ',
        },
        {
          content: ' Response Input ',
        },
        {
          content: ' Header  transform-headers ',
        },
        {
          content: ' Body  policy-assign-content ',
        },
        {
          content: ' Response Output ',
        },
        {
          content: ' Client APP ',
        },
      ]);
    });

    it('should display inspector content for selected policy', () => {
      [...fixture.nativeElement.querySelectorAll('.policy-studio-debug-timeline-card').values()]
        .find((card) => card.textContent.includes('Header  policy-assign-attributes'))
        .click();
      fixture.detectChanges();

      const inspectorContent = fixture.nativeElement.querySelector(
        '.policy-studio-debug-response__display-response__inspector',
      ).textContent;
      const expectedDebugStepAttributes = fakeDebugEvent().payload.debugSteps[1].result.attributes as Record<string, unknown>;
      expect(inspectorContent).toContain(expectedDebugStepAttributes.dev);
    });

    it('should clear inspector selection after new request', fakeAsync(async () => {
      const getInspectorContent = () =>
        fixture.nativeElement.querySelector('.policy-studio-debug-response__display-response__inspector')?.textContent ?? null;

      // Select card and expect inspector content
      [...fixture.nativeElement.querySelectorAll('.policy-studio-debug-timeline-card').values()]
        .find((card) => card.textContent.includes('Header  policy-assign-attributes'))
        .click();
      fixture.detectChanges();
      expect(getInspectorContent()).not.toEqual(null);

      // Send new debug request and expect empty inspector content
      const sendButton = await loader.getHarness(MatButtonHarness.with({ text: 'Send' }));
      await sendButton.click();
      tick();
      expectSendDebugEvent(EVENT_ID);
      tick(1000);
      expectGetDebugEvent(EVENT_ID, true);
      fixture.detectChanges();

      expect(getInspectorContent()).toEqual(null);
    }));
  });

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
        payload: JSON.stringify(fakeDebugEvent().payload),
      });
  }
});
