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
import { ActivatedRoute } from '@angular/router';

import { DebugModeComponent } from './debug-mode.component';
import { DebugModeModule } from './debug-mode.module';
import { fakeDebugEvent } from './models/DebugEvent.fixture';
import { DebugModeV2Service } from './v2-wrapper/debug-mode-v2.service';
import { DebugModeV4Service } from './v4-wrapper/debug-mode-v4.service';
import { DebugModeService } from './debug-mode.service';

import { PolicyStudioService } from '../policy-studio-v2/policy-studio.service';
import { fakePolicyListItem } from '../../../entities/policy';
import { fakeApiV2, fakeApiV4 } from '../../../entities/management-api-v2';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { toApiDefinition } from '../policy-studio-v2/models/ApiDefinition';

describe('PolicyStudioDebugComponent with DebugModeV2Service', () => {
  let fixture: ComponentFixture<DebugModeComponent>;
  let httpTestingController: HttpTestingController;
  let policyStudioService: PolicyStudioService;
  let loader: HarnessLoader;

  const policies = [
    fakePolicyListItem(),
    fakePolicyListItem({
      id: 'key-less',
      name: 'Keyless',
    }),
    fakePolicyListItem({
      id: 'policy-assign-attributes',
      name: 'Assign Attributes',
    }),
    fakePolicyListItem({
      id: 'policy-override-request-method',
      name: undefined,
    }),
    fakePolicyListItem({
      id: 'transform-headers',
      name: 'Transform Headers',
    }),
  ];
  const api = fakeApiV2();

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, DebugModeModule, MatIconTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: api.id } } } },
        { provide: DebugModeService, useClass: DebugModeV2Service },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(DebugModeComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    httpTestingController = TestBed.inject(HttpTestingController);

    policyStudioService = TestBed.inject(PolicyStudioService);
    policyStudioService.setApiDefinition(toApiDefinition(api));
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
        state: card.querySelector('.policy-studio-debug-timeline-card__right').innerHTML.includes('gio:alert-circle')
          ? 'ERROR'
          : card.querySelector('.policy-studio-debug-timeline-card__right').innerHTML.includes('gio:alert-circle')
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
          content: ' Plan > Header  Keyless ',
        },
        {
          content: ' Api > Header  Assign Attributes ',
        },
        {
          content: ' Api > Header  Policy Override Request Method ',
        },
        {
          content: ' Api > Header  Transform Headers ',
        },
        {
          content: ' Api > Header  Transform Headers ',
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
          content: ' Api > Header  Transform Headers ',
        },
        {
          content: ' Api > Body  policy-assign-content ',
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
        .find((card) => card.textContent.includes('Header  Assign Attributes'))
        .click();
      fixture.detectChanges();

      const inspectorContent = fixture.nativeElement.querySelector(
        '.policy-studio-debug-response__display-response__inspector',
      ).textContent;
      const expectedDebugStepAttributes = fakeDebugEvent().payload.debugSteps[1].result.attributes as Record<string, unknown>;
      expect(inspectorContent).toContain(expectedDebugStepAttributes.dev);
    });

    it('should display inspector content on selected policy with overview', () => {
      [...fixture.nativeElement.querySelectorAll('.policy-studio-debug-timeline-card').values()]
        .find((card) => card.textContent.includes('Header  Assign Attributes'))
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
        .find((card) => card.textContent.includes('Header  Assign Attributes'))
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

describe('PolicyStudioDebugComponent with DebugModeV4Service', () => {
  let fixture: ComponentFixture<DebugModeComponent>;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;

  const policies = [
    fakePolicyListItem(),
    fakePolicyListItem({
      id: 'key-less',
      name: 'Keyless',
    }),
    fakePolicyListItem({
      id: 'policy-assign-attributes',
      name: 'Assign Attributes',
    }),
    fakePolicyListItem({
      id: 'policy-override-request-method',
      name: undefined,
    }),
    fakePolicyListItem({
      id: 'transform-headers',
      name: 'Transform Headers',
    }),
  ];
  const api = fakeApiV4();

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, DebugModeModule, MatIconTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { apiId: api.id } } } },
        { provide: DebugModeService, useClass: DebugModeV4Service },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(DebugModeComponent);
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
        state: card.querySelector('.policy-studio-debug-timeline-card__right').innerHTML.includes('gio:alert-circle')
          ? 'ERROR'
          : card.querySelector('.policy-studio-debug-timeline-card__right').innerHTML.includes('gio:alert-circle')
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
          content: ' Plan > Header  Keyless ',
        },
        {
          content: ' Api > Header  Assign Attributes ',
        },
        {
          content: ' Api > Header  Policy Override Request Method ',
        },
        {
          content: ' Api > Header  Transform Headers ',
        },
        {
          content: ' Api > Header  Transform Headers ',
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
          content: ' Api > Header  Transform Headers ',
        },
        {
          content: ' Api > Body  policy-assign-content ',
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
        .find((card) => card.textContent.includes('Header  Assign Attributes'))
        .click();
      fixture.detectChanges();

      const inspectorContent = fixture.nativeElement.querySelector(
        '.policy-studio-debug-response__display-response__inspector',
      ).textContent;
      const expectedDebugStepAttributes = fakeDebugEvent().payload.debugSteps[1].result.attributes as Record<string, unknown>;
      expect(inspectorContent).toContain(expectedDebugStepAttributes.dev);
    });

    it('should display inspector content on selected policy with overview', () => {
      [...fixture.nativeElement.querySelectorAll('.policy-studio-debug-timeline-card').values()]
        .find((card) => card.textContent.includes('Header  Assign Attributes'))
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
        .find((card) => card.textContent.includes('Header  Assign Attributes'))
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
    httpTestingController
      .expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}/debug`,
      })
      .flush({ id: eventId });
  }

  function expectGetDebugEvent(eventId: string, success: boolean) {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}/events/${eventId}`,
      })
      .flush({
        type: 'DEBUG_API',
        properties: {
          API_DEBUG_STATUS: success ? 'SUCCESS' : 'FAILED',
        },
        payload: JSON.stringify(fakeDebugEvent().payload),
      });
  }
});
