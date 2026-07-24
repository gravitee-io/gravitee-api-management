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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { FailedHealthCheckDetailsDialogComponent } from './failed-health-check-details-dialog.component';
import { FailedHealthCheckDetailsDialogHarness } from './failed-health-check-details-dialog.harness';

import { fakeApiHealthCheckLogs, fakeHealthCheckStep } from '../../../../../../entities/management-api-v2/api/v4/healthCheck.fixture';
import { HealthCheckLog } from '../../../../../../entities/management-api-v2/api/v4/healthCheck';

describe('FailedHealthCheckDetailsDialogComponent', () => {
  let fixture: ComponentFixture<FailedHealthCheckDetailsDialogComponent>;
  let harness: FailedHealthCheckDetailsDialogHarness;

  const aLog = (attribute?: Partial<HealthCheckLog>): HealthCheckLog => ({
    ...fakeApiHealthCheckLogs().data[0],
    ...attribute,
  });

  const init = async (log: HealthCheckLog) => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, MatIconTestingModule, FailedHealthCheckDetailsDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: { close: jest.fn() } },
        { provide: MAT_DIALOG_DATA, useValue: log },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(FailedHealthCheckDetailsDialogComponent);
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, FailedHealthCheckDetailsDialogHarness);
  };

  afterEach(() => TestBed.resetTestingModule());

  describe('summary', () => {
    beforeEach(async () => await init(aLog()));

    it('should_display_log_summary', async () => {
      expect(await harness.getSummaryValue('timestamp')).toEqual('2024-11-13T15:50:41Z');
      expect(await harness.getSummaryValue('endpoint')).toEqual('sample-endpoint-name');
      expect(await harness.getSummaryValue('gateway')).toEqual('sample-gateway-id');
      expect(await harness.getSummaryValue('response-time')).toEqual('150ms');
    });
  });

  describe('step details', () => {
    beforeEach(async () => await init(aLog()));

    it('should_display_request_method_and_uri', async () => {
      expect(await harness.getRequestMethod()).toEqual('get');
      expect(await harness.getRequestUri()).toEqual('https://backend:8080/_health');
    });

    it('should_display_request_headers', async () => {
      expect(await harness.getRequestHeaders()).toEqual([{ name: 'Accept', value: 'application/json' }]);
    });

    it('should_display_response_status', async () => {
      expect(await harness.getResponseStatus()).toEqual('503');
    });

    it('should_display_response_headers', async () => {
      expect(await harness.getResponseHeaders()).toEqual([{ name: 'Content-Type', value: 'text/plain' }]);
    });

    it('should_display_assertion_message', async () => {
      expect(await harness.getStepMessages()).toEqual(['Assertion not validated: status code is 503, expected 200']);
    });

    it('should_display_response_body', async () => {
      expect(await harness.hasBodySection()).toBe(true);
    });
  });

  describe('sensitive headers', () => {
    it('should_mask_sensitive_request_headers', async () => {
      await init(
        aLog({
          steps: [
            fakeHealthCheckStep({
              request: {
                uri: 'https://backend:8080/_health',
                method: 'GET',
                headers: {
                  Authorization: 'Bearer eyJhbGciOiJIUzI1NiJ9.payload.signature',
                  Accept: 'application/json',
                },
              },
            }),
          ],
        }),
      );

      expect(await harness.getRequestHeaders()).toEqual([
        { name: 'Authorization', value: 'Bearer ••••••••ture' },
        { name: 'Accept', value: 'application/json' },
      ]);
    });

    it('should_mask_sensitive_response_headers', async () => {
      await init(
        aLog({
          steps: [
            fakeHealthCheckStep({
              response: {
                status: 503,
                body: '',
                headers: { 'Set-Cookie': 'sessionId=some-very-long-session-value; HttpOnly' },
              },
            }),
          ],
        }),
      );

      expect(await harness.getResponseHeaders()).toEqual([{ name: 'Set-Cookie', value: '••••••••' }]);
    });
  });

  describe('edge cases', () => {
    it('should_display_no_headers_message_when_headers_are_empty', async () => {
      await init(
        aLog({
          steps: [
            fakeHealthCheckStep({
              request: { uri: 'https://backend:8080/_health', method: 'GET', headers: {} },
              response: { status: 503, body: 'Service Unavailable', headers: {} },
            }),
          ],
        }),
      );

      expect(await harness.hasRequestNoHeadersMessage()).toBe(true);
      expect(await harness.hasResponseNoHeadersMessage()).toBe(true);
    });

    it('should_not_display_body_section_when_body_is_absent', async () => {
      await init(
        aLog({
          steps: [fakeHealthCheckStep({ response: { status: 503, body: undefined, headers: {} } })],
        }),
      );

      expect(await harness.hasBodySection()).toBe(false);
    });

    it('should_display_one_section_per_step_when_multiple_steps', async () => {
      await init(
        aLog({
          steps: [fakeHealthCheckStep({ name: 'first-step' }), fakeHealthCheckStep({ name: 'second-step' })],
        }),
      );

      expect(await harness.getStepCount()).toEqual(2);
      expect(await harness.getStepNames()).toEqual(['first-step', 'second-step']);
    });

    it('should_display_empty_state_when_no_steps', async () => {
      await init(aLog({ steps: [] }));

      expect(await harness.getStepCount()).toEqual(0);
      expect(await harness.hasNoDetailsMessage()).toBe(true);
    });

    it('should_display_empty_state_when_steps_are_undefined', async () => {
      await init(aLog({ steps: undefined }));

      expect(await harness.hasNoDetailsMessage()).toBe(true);
    });

    it('should_not_render_message_row_when_message_is_absent', async () => {
      await init(aLog({ steps: [fakeHealthCheckStep({ message: undefined })] }));

      expect(await harness.getStepMessages()).toEqual([]);
    });
  });

  describe('closing', () => {
    beforeEach(async () => await init(aLog()));

    it('should_close_dialog_when_close_button_clicked', async () => {
      await harness.close();

      expect(TestBed.inject(MatDialogRef).close).toHaveBeenCalled();
    });
  });
});
