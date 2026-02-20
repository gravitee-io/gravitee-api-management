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
import { DEBUG_EVENT_FAILED_MESSAGE, DebugModeService } from './debug-mode.service';

import { PolicyStudioService } from '../policy-studio-v2/policy-studio.service';
import { fakePolicyListItem } from '../../../entities/policy';
import { fakeApiV2, fakeApiV4 } from '../../../entities/management-api-v2';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { toApiDefinition } from '../policy-studio-v2/models/ApiDefinition';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

const LONG_REQUEST_WARNING_TEXT =
  'The debug request has taken more than 20 seconds. It is unlikely that a response will be received, but we will continue waiting.';

describe('DebugModeComponent with DebugModeV2Service', () => {
  let fixture: ComponentFixture<DebugModeComponent>;
  let httpTestingController: HttpTestingController;
  let policyStudioService: PolicyStudioService;
  let snackBarService: SnackBarService;
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
    snackBarService = TestBed.inject(SnackBarService);

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
    expectGetDebugEvent(eventId, 'DEBUGGING');
    tick(1000);
    expectGetDebugEvent(eventId, 'SUCCESS');

    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.debug-mode-response__display-response__header').textContent).toContain(
      'Response 200 - OK  GET /',
    );
  }));

  it('should display a warning only after 20 seconds while request is still loading', fakeAsync(async () => {
    const eventId = 'eventId';

    const sendButton = await loader.getHarness(MatButtonHarness.with({ text: 'Send' }));
    const cancelButton = await loader.getHarness(MatButtonHarness.with({ text: 'Cancel' }));

    await sendButton.click();
    tick();
    expectSendDebugEvent(eventId);

    for (let i = 0; i < 19; i++) {
      tick(1000);
      expectGetDebugEvent(eventId, 'DEBUGGING');
    }

    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain(LONG_REQUEST_WARNING_TEXT);

    tick(1000);
    expectGetDebugEvent(eventId, 'DEBUGGING');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(LONG_REQUEST_WARNING_TEXT);

    await cancelButton.click();
    tick();
  }));

  it('should display late success response and clear the warning state', fakeAsync(async () => {
    const eventId = 'eventId';

    const sendButton = await loader.getHarness(MatButtonHarness.with({ text: 'Send' }));

    await sendButton.click();
    tick();
    expectSendDebugEvent(eventId);

    for (let i = 0; i < 30; i++) {
      tick(1000);
      expectGetDebugEvent(eventId, 'DEBUGGING');
    }
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(LONG_REQUEST_WARNING_TEXT);

    for (let i = 0; i < 14; i++) {
      tick(1000);
      expectGetDebugEvent(eventId, 'DEBUGGING');
    }

    tick(1000);
    expectGetDebugEvent(eventId, 'SUCCESS');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.debug-mode-response__display-response__header').textContent).toContain(
      'Response 200 - OK  GET /',
    );
    expect(fixture.nativeElement.textContent).not.toContain(LONG_REQUEST_WARNING_TEXT);
  }));

  it('should stop polling when component is destroyed', fakeAsync(async () => {
    const eventId = 'eventId';

    const sendButton = await loader.getHarness(MatButtonHarness.with({ text: 'Send' }));

    await sendButton.click();
    tick();
    expectSendDebugEvent(eventId);

    tick(1000);
    expectGetDebugEvent(eventId, 'DEBUGGING');

    fixture.destroy();
    tick(5000);

    httpTestingController.expectNone({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}/events/${eventId}`,
    });
  }));

  it('should stop polling and clear warning when request is cancelled', fakeAsync(async () => {
    const eventId = 'eventId';

    const sendButton = await loader.getHarness(MatButtonHarness.with({ text: 'Send' }));
    const cancelButton = await loader.getHarness(MatButtonHarness.with({ text: 'Cancel' }));

    await sendButton.click();
    tick();
    expectSendDebugEvent(eventId);

    for (let i = 0; i < 30; i++) {
      tick(1000);
      expectGetDebugEvent(eventId, 'DEBUGGING');
    }

    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(LONG_REQUEST_WARNING_TEXT);

    await cancelButton.click();
    tick();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Define and run the request you want to test!');
    expect(fixture.nativeElement.textContent).not.toContain(LONG_REQUEST_WARNING_TEXT);

    tick(5000);
    httpTestingController.expectNone({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}/events/${eventId}`,
    });
  }));

  it('should allow a new request to be sent after a previous one was cancelled', fakeAsync(async () => {
    const eventId = 'eventId';

    const sendButton = await loader.getHarness(MatButtonHarness.with({ text: 'Send' }));
    const cancelButton = await loader.getHarness(MatButtonHarness.with({ text: 'Cancel' }));

    // First request — cancel it
    await sendButton.click();
    tick();
    expectSendDebugEvent(eventId);
    tick(1000);
    expectGetDebugEvent(eventId, 'DEBUGGING');

    await cancelButton.click();
    tick();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Define and run the request you want to test!');

    // Second request — must not be immediately cancelled by the old Subject emission
    await sendButton.click();
    tick();
    expectSendDebugEvent(eventId);
    tick(1000);
    expectGetDebugEvent(eventId, 'DEBUGGING');
    tick(1000);
    expectGetDebugEvent(eventId, 'SUCCESS');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.debug-mode-response__display-response__header').textContent).toContain(
      'Response 200 - OK  GET /',
    );
  }));

  it('should stop polling and display an error message when debug event status is error', fakeAsync(async () => {
    const eventId = 'eventId';
    const snackBarErrorSpy = jest.spyOn(snackBarService, 'error').mockImplementation();

    const sendButton = await loader.getHarness(MatButtonHarness.with({ text: 'Send' }));
    await sendButton.click();

    tick();
    expectSendDebugEvent(eventId);
    tick(1000);
    expectGetDebugEvent(eventId, 'ERROR');
    tick();

    expect(snackBarErrorSpy).toHaveBeenCalledWith(DEBUG_EVENT_FAILED_MESSAGE);
    tick(5000);
    httpTestingController.expectNone({
      method: 'GET',
      url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}/events/${eventId}`,
    });
  }));

  describe('when debug response is success', () => {
    const EVENT_ID = 'eventId';

    beforeEach(fakeAsync(async () => {
      const sendButton = await loader.getHarness(MatButtonHarness.with({ text: 'Send' }));
      await sendButton.click();

      tick();
      expectSendDebugEvent(EVENT_ID);
      tick(1000);
      expectGetDebugEvent(EVENT_ID, 'SUCCESS');

      fixture.detectChanges();
    }));

    it('should display timeline', async () => {
      const timeLineCards = [...fixture.nativeElement.querySelectorAll('.debug-mode-timeline-card').values()].map(card => {
        const content = card.querySelector('.debug-mode-timeline-card__content').textContent;
        const state = card.querySelector('mat-icon.error') ? 'ERROR' : card.querySelector('mat-icon.skipped') ? 'SKIPPED' : undefined;
        return {
          content,
          ...(state ? { state } : {}),
        };
      });
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
          state: 'SKIPPED',
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
      [...fixture.nativeElement.querySelectorAll('.debug-mode-timeline-card').values()]
        .find(card => card.textContent.includes('Header  Assign Attributes'))
        .click();
      fixture.detectChanges();

      const inspectorContent = fixture.nativeElement.querySelector('.debug-mode-response__display-response__inspector').textContent;
      const expectedDebugStepAttributes = fakeDebugEvent().payload.debugSteps[1].result.attributes as Record<string, unknown>;
      expect(inspectorContent).toContain(expectedDebugStepAttributes.dev);
    });

    it('should display inspector content on selected policy with overview', () => {
      [...fixture.nativeElement.querySelectorAll('.debug-mode-timeline-card').values()]
        .find(card => card.textContent.includes('Header  Assign Attributes'))
        .click();
      fixture.detectChanges();

      const inspectorContent = fixture.nativeElement.querySelector('.debug-mode-response__display-response__inspector').textContent;
      const expectedDebugStepAttributes = fakeDebugEvent().payload.debugSteps[1].result.attributes as Record<string, unknown>;
      expect(inspectorContent).toContain(expectedDebugStepAttributes.dev);
    });

    it('should clear inspector selection after new request', fakeAsync(async () => {
      const getInspectorContent = () =>
        fixture.nativeElement.querySelector('.debug-mode-response__display-response__inspector')?.textContent ?? null;

      // Select card and expect inspector content
      [...fixture.nativeElement.querySelectorAll('.debug-mode-timeline-card').values()]
        .find(card => card.textContent.includes('Header  Assign Attributes'))
        .click();
      fixture.detectChanges();
      expect(getInspectorContent()).not.toEqual(null);

      // Send new debug request and expect empty inspector content
      const sendButton = await loader.getHarness(MatButtonHarness.with({ text: 'Send' }));
      await sendButton.click();
      tick();
      expectSendDebugEvent(EVENT_ID);
      tick(1000);
      expectGetDebugEvent(EVENT_ID, 'SUCCESS');
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

  function expectGetDebugEvent(eventId: string, status: 'SUCCESS' | 'ERROR' | 'DEBUGGING') {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.baseURL}/apis/${api.id}/events/${eventId}`,
      })
      .flush({
        type: 'DEBUG_API',
        properties: {
          api_debug_status: status,
        },
        payload: JSON.stringify(fakeDebugEvent().payload),
      });
  }
});

describe('DebugModeComponent with DebugModeV4Service', () => {
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
    expectGetDebugEvent(eventId, 'DEBUGGING');
    tick(1000);
    expectGetDebugEvent(eventId, 'SUCCESS');

    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.debug-mode-response__display-response__header').textContent).toContain(
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
      expectGetDebugEvent(EVENT_ID, 'SUCCESS');

      fixture.detectChanges();
    }));

    it('should display timeline', () => {
      const timeLineCards = [...fixture.nativeElement.querySelectorAll('.debug-mode-timeline-card').values()].map(card => {
        const content = card.querySelector('.debug-mode-timeline-card__content').textContent;
        const state = card.querySelector('mat-icon.error') ? 'ERROR' : card.querySelector('mat-icon.skipped') ? 'SKIPPED' : undefined;
        return {
          content,
          ...(state ? { state } : {}),
        };
      });
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
          state: 'SKIPPED',
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
      [...fixture.nativeElement.querySelectorAll('.debug-mode-timeline-card').values()]
        .find(card => card.textContent.includes('Header  Assign Attributes'))
        .click();
      fixture.detectChanges();

      const inspectorContent = fixture.nativeElement.querySelector('.debug-mode-response__display-response__inspector').textContent;
      const expectedDebugStepAttributes = fakeDebugEvent().payload.debugSteps[1].result.attributes as Record<string, unknown>;
      expect(inspectorContent).toContain(expectedDebugStepAttributes.dev);
    });

    it('should display inspector content on selected policy with overview', () => {
      [...fixture.nativeElement.querySelectorAll('.debug-mode-timeline-card').values()]
        .find(card => card.textContent.includes('Header  Assign Attributes'))
        .click();
      fixture.detectChanges();

      const inspectorContent = fixture.nativeElement.querySelector('.debug-mode-response__display-response__inspector').textContent;
      const expectedDebugStepAttributes = fakeDebugEvent().payload.debugSteps[1].result.attributes as Record<string, unknown>;
      expect(inspectorContent).toContain(expectedDebugStepAttributes.dev);
    });

    it('should clear inspector selection after new request', fakeAsync(async () => {
      const getInspectorContent = () =>
        fixture.nativeElement.querySelector('.debug-mode-response__display-response__inspector')?.textContent ?? null;

      // Select card and expect inspector content
      [...fixture.nativeElement.querySelectorAll('.debug-mode-timeline-card').values()]
        .find(card => card.textContent.includes('Header  Assign Attributes'))
        .click();
      fixture.detectChanges();
      expect(getInspectorContent()).not.toEqual(null);

      // Send new debug request and expect empty inspector content
      const sendButton = await loader.getHarness(MatButtonHarness.with({ text: 'Send' }));
      await sendButton.click();
      tick();
      expectSendDebugEvent(EVENT_ID);
      tick(1000);
      expectGetDebugEvent(EVENT_ID, 'SUCCESS');
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

  function expectGetDebugEvent(eventId: string, status: 'SUCCESS' | 'ERROR' | 'DEBUGGING') {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}/events/${eventId}`,
      })
      .flush({
        type: 'DEBUG_API',
        properties: {
          API_DEBUG_STATUS: status,
        },
        payload: JSON.stringify(fakeDebugEvent().payload),
      });
  }
});
