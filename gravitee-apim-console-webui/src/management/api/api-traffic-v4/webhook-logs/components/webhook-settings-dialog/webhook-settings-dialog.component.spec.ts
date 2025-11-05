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
import { HttpTestingController } from '@angular/common/http/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { WebhookSettingsDialogComponent } from './webhook-settings-dialog.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../../shared/testing';
import { ApiV4, fakeApiV4 } from '../../../../../../entities/management-api-v2';

describe('WebhookSettingsDialogComponent', () => {
  const API_ID = 'test-api-id';
  const testApi: ApiV4 = fakeApiV4({
    id: API_ID,
    definitionVersion: 'V4',
    type: 'PROXY',
    name: 'Test API',
    apiVersion: '1',
    analytics: {
      enabled: true,
      logging: {
        content: {
          messagePayload: false,
          messageHeaders: false,
          headers: false,
          payload: false,
        },
      },
      sampling: { type: 'COUNT', value: '100' },
    },
    definitionContext: {
      origin: 'MANAGEMENT',
    },
  });

  let component: WebhookSettingsDialogComponent;
  let fixture: ComponentFixture<WebhookSettingsDialogComponent>;
  let httpTestingController: HttpTestingController;
  let dialogRef: MatDialogRef<WebhookSettingsDialogComponent>;

  const initComponent = (api: ApiV4 = testApi) => {
    fixture = TestBed.createComponent(WebhookSettingsDialogComponent);
    component = fixture.componentInstance;
    dialogRef = TestBed.inject(MatDialogRef);

    fixture.detectChanges();

    const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`);
    expect(req.request.method).toBe('GET');
    req.flush(api);

    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WebhookSettingsDialogComponent, NoopAnimationsModule, GioTestingModule, MatIconTestingModule],
      providers: [
        {
          provide: MatDialogRef,
          useValue: {
            close: jest.fn(),
          },
        },
        {
          provide: MAT_DIALOG_DATA,
          useValue: API_ID,
        },
      ],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('Component Initialization', () => {
    it('should create', () => {
      initComponent();

      expect(component).toBeTruthy();
    });

    it('should load API and initialize form', () => {
      initComponent();

      expect(component.isLoading).toBe(false);
      expect(component.form).toBeDefined();
      expect(component.form.value).toEqual({
        enabled: true,
        requestBody: false,
        requestHeaders: false,
        responseBody: false,
        responseHeaders: false,
        samplingType: 'COUNT',
        samplingValue: '100',
        messageCount: 1,
        timeWindow: 1,
      });
    });
  });

  describe('Form Controls Enable/Disable', () => {
    it('should disable all controls when analytics is disabled', () => {
      initComponent();

      component.form.patchValue({ enabled: false });

      expect(component.form.get('requestBody').disabled).toBe(true);
      expect(component.form.get('requestHeaders').disabled).toBe(true);
      expect(component.form.get('responseBody').disabled).toBe(true);
      expect(component.form.get('responseHeaders').disabled).toBe(true);
      expect(component.form.get('samplingType').disabled).toBe(true);
      expect(component.form.get('samplingValue').disabled).toBe(true);
    });

    it('should enable all controls when analytics is enabled', () => {
      const disabledApi = fakeApiV4({
        ...testApi,
        analytics: { ...testApi.analytics, enabled: false },
      });
      initComponent(disabledApi);

      component.form.patchValue({ enabled: true });

      expect(component.form.get('requestBody').disabled).toBe(false);
      expect(component.form.get('requestHeaders').disabled).toBe(false);
      expect(component.form.get('responseBody').disabled).toBe(false);
      expect(component.form.get('responseHeaders').disabled).toBe(false);
      expect(component.form.get('samplingType').disabled).toBe(false);
      expect(component.form.get('samplingValue').disabled).toBe(false);
    });

    it('should disable all controls when API is from KUBERNETES', () => {
      const k8sApi = fakeApiV4({
        ...testApi,
        definitionContext: { origin: 'KUBERNETES' },
      });
      initComponent(k8sApi);

      expect(component.form.get('enabled').disabled).toBe(true);
      expect(component.form.get('requestBody').disabled).toBe(true);
      expect(component.form.get('samplingType').disabled).toBe(true);
    });
  });

  describe('Sampling Type Changes', () => {
    it('should update validators when switching to PROBABILITY', () => {
      initComponent();
      component.form.patchValue({ samplingType: 'PROBABILITY' });

      expect(component.form.get('samplingValue').value).toBe('0.01');
      expect(component.form.get('samplingValue').hasError('min')).toBe(false);

      component.form.patchValue({ samplingValue: 0.001 });
      expect(component.form.get('samplingValue').hasError('min')).toBe(true);

      component.form.patchValue({ samplingValue: 0.6 });
      expect(component.form.get('samplingValue').hasError('max')).toBe(true);
    });

    it('should update validators when switching to COUNT', () => {
      initComponent();
      component.form.patchValue({ samplingType: 'COUNT' });

      expect(component.form.get('samplingValue').value).toBe('100');

      component.form.patchValue({ samplingValue: 5 });
      expect(component.form.get('samplingValue').hasError('min')).toBe(true);

      component.form.patchValue({ samplingValue: 100 });
      expect(component.form.get('samplingValue').hasError('min')).toBe(false);
    });

    it('should update validators when switching to TEMPORAL', () => {
      initComponent();
      component.form.patchValue({ samplingType: 'TEMPORAL' });

      expect(component.form.get('samplingValue').value).toBe('PT1S');
    });

    it('should set default values when switching to COUNT_PER_TIME_WINDOW', () => {
      initComponent();
      component.form.patchValue({ samplingType: 'COUNT_PER_TIME_WINDOW' });

      expect(component.form.get('messageCount').value).toBe(1);
      expect(component.form.get('timeWindow').value).toBe(1);
    });
  });

  describe('Save Functionality', () => {
    it('should save analytics settings with PROBABILITY type', () => {
      initComponent();
      component.form.patchValue({
        enabled: true,
        requestBody: true,
        requestHeaders: true,
        samplingType: 'PROBABILITY',
        samplingValue: '0.5',
      });

      component.save();

      const getReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${testApi.id}`);
      expect(getReq.request.method).toBe('GET');
      getReq.flush(testApi);

      expectApiPutRequest({
        ...testApi,
        analytics: {
          ...testApi.analytics,
          enabled: true,
          logging: {
            ...testApi.analytics.logging,
            content: {
              ...testApi.analytics.logging.content,
              messagePayload: true,
              messageHeaders: true,
              headers: false,
              payload: false,
            },
          },
          sampling: { type: 'PROBABILITY', value: '0.5' },
        },
      });

      expect(dialogRef.close).toHaveBeenCalledWith({ saved: true });
    });

    it('should save analytics settings with COUNT type', () => {
      initComponent();
      component.form.patchValue({
        samplingType: 'COUNT',
        samplingValue: '50',
      });

      component.save();

      const getReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${testApi.id}`);
      expect(getReq.request.method).toBe('GET');
      getReq.flush(testApi);

      expectApiPutRequest({
        ...testApi,
        analytics: {
          ...testApi.analytics,
          sampling: { type: 'COUNT', value: '50' },
        },
      });

      expect(dialogRef.close).toHaveBeenCalledWith({ saved: true });
    });

    it('should save COUNT_PER_TIME_WINDOW as COUNT with slash-separated value', () => {
      initComponent();
      component.form.patchValue({
        samplingType: 'COUNT_PER_TIME_WINDOW',
        messageCount: 10,
        timeWindow: 60,
      });

      component.save();

      const getReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${testApi.id}`);
      expect(getReq.request.method).toBe('GET');
      getReq.flush(testApi);

      expectApiPutRequest({
        ...testApi,
        analytics: {
          ...testApi.analytics,
          sampling: { type: 'COUNT', value: '10/60' },
        },
      });
    });

    it('should not save when form is invalid', () => {
      initComponent();
      component.form.patchValue({ samplingValue: null });
      component.form.get('samplingValue').markAsTouched();

      component.save();

      expect(dialogRef.close).not.toHaveBeenCalled();
    });

    it('should update form state after successful save', () => {
      initComponent();
      component.save();

      const getReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/apis/${testApi.id}`);
      expect(getReq.request.method).toBe('GET');
      getReq.flush(testApi);

      expectApiPutRequest(testApi);

      expect(component.form.pristine).toBe(true);
      expect(component.initialFormValue).toEqual(component.form.getRawValue());
    });
  });

  describe('Cancel Functionality', () => {
    it('should close dialog without data on cancel', () => {
      initComponent();

      component.cancel();

      expect(dialogRef.close).toHaveBeenCalledWith();
    });
  });

  describe('Load Existing COUNT_PER_TIME_WINDOW', () => {
    it('should parse COUNT with slash value as COUNT_PER_TIME_WINDOW', () => {
      const apiWithTimeWindow = fakeApiV4({
        ...testApi,
        analytics: {
          ...testApi.analytics,
          sampling: { type: 'COUNT', value: '50/120' },
        },
      });

      initComponent(apiWithTimeWindow);

      expect(component.form.value.samplingType).toBe('COUNT_PER_TIME_WINDOW');
      expect(component.form.value.messageCount).toBe(50);
      expect(component.form.value.timeWindow).toBe(120);
    });
  });

  describe('Helper Methods', () => {
    it('should return correct default values for each sampling type', () => {
      initComponent();
      expect(component['getSamplingDefaultValue']('PROBABILITY')).toBe('0.01');
      expect(component['getSamplingDefaultValue']('COUNT')).toBe('100');
      expect(component['getSamplingDefaultValue']('TEMPORAL')).toBe('PT1S');
      expect(component['getSamplingDefaultValue']('COUNT_PER_TIME_WINDOW')).toBe('');
    });

    it('should return correct validators for each sampling type', () => {
      initComponent();
      const probValidators = component['getSamplingValueValidators']('PROBABILITY');
      expect(probValidators.length).toBeGreaterThan(0);

      const countValidators = component['getSamplingValueValidators']('COUNT');
      expect(countValidators.length).toBeGreaterThan(0);

      const temporalValidators = component['getSamplingValueValidators']('TEMPORAL');
      expect(temporalValidators.length).toBeGreaterThan(0);
    });
  });

  function expectApiPutRequest(api: ApiV4) {
    const req = httpTestingController.expectOne({
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/apis/${api.id}`,
      method: 'PUT',
    });
    expect(req.request.body).toStrictEqual(api);
    req.flush(api);
  }
});
