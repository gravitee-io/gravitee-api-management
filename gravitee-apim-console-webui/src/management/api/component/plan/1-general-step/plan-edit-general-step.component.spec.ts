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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { PlanEditGeneralStepComponent } from './plan-edit-general-step.component';

import { ApiPlanFormModule } from '../api-plan-form.module';
import { GioTestingModule, CONSTANTS_TESTING } from '../../../../../shared/testing';
import { fakeNativeKafkaApiV4, fakeApiV4 } from '../../../../../entities/management-api-v2';

describe('PlanEditGeneralStepComponent — Kafka port routing', () => {
  let fixture: ComponentFixture<PlanEditGeneralStepComponent>;
  let component: PlanEditGeneralStepComponent;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const setupComponent = (mode: 'create' | 'edit' = 'create', isNative = false, api = fakeNativeKafkaApiV4()) => {
    TestBed.configureTestingModule({
      declarations: [PlanEditGeneralStepComponent],
      imports: [NoopAnimationsModule, GioTestingModule, ApiPlanFormModule, MatIconTestingModule],
    });

    fixture = TestBed.createComponent(PlanEditGeneralStepComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);

    component.mode = mode;
    component.isNative = isNative;
    component.api = api;
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  const flushDefaultRequests = (apiId: string) => {
    httpTestingController
      .expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/apis/${apiId}/pages?type=MARKDOWN&api=${apiId}` })
      .flush([]);
    httpTestingController.expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.org.baseURL}/configuration/tags` }).flush([]);
    httpTestingController.expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.org.baseURL}/user/tags` }).flush([]);
    httpTestingController.expectOne({ method: 'GET', url: `${CONSTANTS_TESTING.env.baseURL}/configuration/groups` }).flush([]);
    fixture.detectChanges();
  };

  describe('Kafka port routing section visibility', () => {
    it('should render the Kafka port routing section when isNative is true', async () => {
      setupComponent('create', true);
      flushDefaultRequests(fakeNativeKafkaApiV4().id);

      const bootstrapPortInput = await loader.getHarnessOrNull(MatInputHarness.with({ selector: '[data-testid="bootstrap_port_field"]' }));
      expect(bootstrapPortInput).not.toBeNull();

      const brokerRangeStartInput = await loader.getHarnessOrNull(
        MatInputHarness.with({ selector: '[data-testid="broker_range_start_field"]' }),
      );
      expect(brokerRangeStartInput).not.toBeNull();

      const brokerRangeEndInput = await loader.getHarnessOrNull(
        MatInputHarness.with({ selector: '[data-testid="broker_range_end_field"]' }),
      );
      expect(brokerRangeEndInput).not.toBeNull();
    });

    it('should NOT render the Kafka port routing section when isNative is false', async () => {
      const nonNativeApi = fakeApiV4({ type: 'PROXY' });
      setupComponent('create', false, nonNativeApi);
      flushDefaultRequests(nonNativeApi.id);

      const bootstrapPortInput = await loader.getHarnessOrNull(MatInputHarness.with({ selector: '[data-testid="bootstrap_port_field"]' }));
      expect(bootstrapPortInput).toBeNull();
    });
  });

  describe('Port field min/max validation', () => {
    beforeEach(() => {
      setupComponent('create', true);
      flushDefaultRequests(fakeNativeKafkaApiV4().id);
    });

    it('should report a min error when bootstrapPort is below 1024', async () => {
      component.generalForm.get('bootstrapPort').setValue(100);
      component.generalForm.get('bootstrapPort').markAsTouched();
      fixture.detectChanges();

      expect(component.generalForm.get('bootstrapPort').hasError('min')).toBe(true);
    });

    it('should report a max error when bootstrapPort is above 65535', async () => {
      component.generalForm.get('bootstrapPort').setValue(70000);
      component.generalForm.get('bootstrapPort').markAsTouched();
      fixture.detectChanges();

      expect(component.generalForm.get('bootstrapPort').hasError('max')).toBe(true);
    });

    it('should report a min error when brokerRangeStart is below 1024', async () => {
      component.generalForm.get('brokerRangeStart').setValue(500);
      component.generalForm.get('brokerRangeStart').markAsTouched();
      fixture.detectChanges();

      expect(component.generalForm.get('brokerRangeStart').hasError('min')).toBe(true);
    });

    it('should report a max error when brokerRangeEnd is above 65535', async () => {
      component.generalForm.get('brokerRangeEnd').setValue(99999);
      component.generalForm.get('brokerRangeEnd').markAsTouched();
      fixture.detectChanges();

      expect(component.generalForm.get('brokerRangeEnd').hasError('max')).toBe(true);
    });

    it('should have no port errors when values are within valid range', async () => {
      component.generalForm.get('bootstrapPort').setValue(9092);
      component.generalForm.get('brokerRangeStart').setValue(9093);
      component.generalForm.get('brokerRangeEnd').setValue(9095);
      fixture.detectChanges();

      expect(component.generalForm.get('bootstrapPort').valid).toBe(true);
      expect(component.generalForm.get('brokerRangeStart').valid).toBe(true);
      expect(component.generalForm.get('brokerRangeEnd').valid).toBe(true);
    });
  });

  describe('Cross-field validation', () => {
    beforeEach(() => {
      setupComponent('create', true);
      flushDefaultRequests(fakeNativeKafkaApiV4().id);
    });

    it('should report rangeOrder error when brokerRangeStart > brokerRangeEnd', () => {
      component.generalForm.get('brokerRangeStart').setValue(9095);
      component.generalForm.get('brokerRangeEnd').setValue(9093);
      component.generalForm.updateValueAndValidity();
      fixture.detectChanges();

      expect(component.generalForm.hasError('rangeOrder')).toBe(true);
    });

    it('should not report rangeOrder error when brokerRangeStart <= brokerRangeEnd', () => {
      component.generalForm.get('brokerRangeStart').setValue(9093);
      component.generalForm.get('brokerRangeEnd').setValue(9095);
      component.generalForm.updateValueAndValidity();
      fixture.detectChanges();

      expect(component.generalForm.hasError('rangeOrder')).toBe(false);
    });

    it('should report bootstrapInRange error when bootstrapPort is within [rangeStart, rangeEnd]', () => {
      component.generalForm.get('bootstrapPort').setValue(9094);
      component.generalForm.get('brokerRangeStart').setValue(9093);
      component.generalForm.get('brokerRangeEnd').setValue(9095);
      component.generalForm.updateValueAndValidity();
      fixture.detectChanges();

      expect(component.generalForm.hasError('bootstrapInRange')).toBe(true);
    });

    it('should not report bootstrapInRange error when bootstrapPort is outside the broker range', () => {
      component.generalForm.get('bootstrapPort').setValue(9092);
      component.generalForm.get('brokerRangeStart').setValue(9093);
      component.generalForm.get('brokerRangeEnd').setValue(9095);
      component.generalForm.updateValueAndValidity();
      fixture.detectChanges();

      expect(component.generalForm.hasError('bootstrapInRange')).toBe(false);
    });

    it('should not report bootstrapInRange error when range bounds are not set', () => {
      component.generalForm.get('bootstrapPort').setValue(9092);
      component.generalForm.get('brokerRangeStart').setValue(null);
      component.generalForm.get('brokerRangeEnd').setValue(null);
      component.generalForm.updateValueAndValidity();
      fixture.detectChanges();

      expect(component.generalForm.hasError('bootstrapInRange')).toBe(false);
    });
  });

  describe('Broker range change warning banner', () => {
    beforeEach(() => {
      setupComponent('edit', true);
      flushDefaultRequests(fakeNativeKafkaApiV4().id);
    });

    it('should show the broker range change warning banner when showBrokerRangeChangeWarning is true', async () => {
      component.showBrokerRangeChangeWarning = true;
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Changing the broker port range will cause a brief reconnection');
    });

    it('should NOT show the broker range change warning banner when showBrokerRangeChangeWarning is false', async () => {
      component.showBrokerRangeChangeWarning = false;
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).not.toContain('Changing the broker port range will cause a brief reconnection');
    });

    it('should NOT show the broker range change warning banner when isNative is false even if showBrokerRangeChangeWarning is true', () => {
      // The `isNative` flag is true in this describe's beforeEach; toggle it off to simulate non-native.
      component.isNative = false;
      component.showBrokerRangeChangeWarning = true;
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).not.toContain('Changing the broker port range will cause a brief reconnection');
    });
  });

  describe('Cross-field error display (DOM)', () => {
    beforeEach(() => {
      setupComponent('create', true);
      flushDefaultRequests(fakeNativeKafkaApiV4().id);
    });

    it('should DISPLAY the rangeOrder error message on the broker range end field', async () => {
      component.generalForm.get('brokerRangeStart').setValue(9095);
      component.generalForm.get('brokerRangeEnd').setValue(9093);
      component.generalForm.get('brokerRangeStart').markAsTouched();
      component.generalForm.get('brokerRangeEnd').markAsTouched();
      component.generalForm.updateValueAndValidity();
      fixture.detectChanges();

      const endField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Broker range end' }));
      expect((await endField.getTextErrors()).join(' ')).toContain('less than or equal to broker range end');
    });

    it('should DISPLAY the bootstrapInRange error message on the bootstrap port field', async () => {
      component.generalForm.get('bootstrapPort').setValue(9094);
      component.generalForm.get('brokerRangeStart').setValue(9093);
      component.generalForm.get('brokerRangeEnd').setValue(9095);
      component.generalForm.get('bootstrapPort').markAsTouched();
      component.generalForm.updateValueAndValidity();
      fixture.detectChanges();

      const bootstrapField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Bootstrap port' }));
      expect((await bootstrapField.getTextErrors()).join(' ')).toContain('must not fall within the broker port range');
    });
  });
});
