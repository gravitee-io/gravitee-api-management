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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { PlanCardComponent } from './plan-card.component';
import { fakePlan } from '../../../entities/plan/plan.fixture';

describe('PlanCardComponent', () => {
  let component: PlanCardComponent;
  let fixture: ComponentFixture<PlanCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PlanCardComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(PlanCardComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('plan', fakePlan({ security: 'API_KEY', mode: 'STANDARD' }));
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('authentication()', () => {
    it('should return null for PUSH mode plan', () => {
      fixture.componentRef.setInput('plan', fakePlan({ mode: 'PUSH' }));
      fixture.detectChanges();

      expect(component.authentication()).toBeNull();
    });

    it('should return SASL/SSL PLAIN label for NATIVE API with API_KEY security', () => {
      component.apiType = 'NATIVE';
      fixture.componentRef.setInput('plan', fakePlan({ security: 'API_KEY', mode: 'STANDARD' }));
      fixture.detectChanges();

      expect(component.authentication()).toBe('SASL/SSL with SASL mechanisms PLAIN, SCRAM-256, and SCRAM-512');
    });

    it('should return SASL/SSL OAUTHBEARER label for NATIVE API with JWT security', () => {
      component.apiType = 'NATIVE';
      fixture.componentRef.setInput('plan', fakePlan({ security: 'JWT', mode: 'STANDARD' }));
      fixture.detectChanges();

      expect(component.authentication()).toBe('SASL/SSL with SASL mechanism OAUTHBEARER');
    });

    it('should return SASL/SSL OAUTHBEARER label for NATIVE API with OAUTH2 security', () => {
      component.apiType = 'NATIVE';
      fixture.componentRef.setInput('plan', fakePlan({ security: 'OAUTH2', mode: 'STANDARD' }));
      fixture.detectChanges();

      expect(component.authentication()).toBe('SASL/SSL with SASL mechanism OAUTHBEARER');
    });

    it('should return SSL for NATIVE API with KEY_LESS security (default case)', () => {
      component.apiType = 'NATIVE';
      fixture.componentRef.setInput('plan', fakePlan({ security: 'KEY_LESS', mode: 'STANDARD' }));
      fixture.detectChanges();

      expect(component.authentication()).toBe('SSL');
    });

    it('should return standard security label for non-NATIVE API', () => {
      component.apiType = 'PROXY';
      fixture.componentRef.setInput('plan', fakePlan({ security: 'API_KEY', mode: 'STANDARD' }));
      fixture.detectChanges();

      expect(component.authentication()).toBe('API Key');
    });
  });

  describe('onSelectPlan()', () => {
    it('should emit selectPlan when not disabled', () => {
      const plan = fakePlan({ security: 'API_KEY', mode: 'STANDARD' });
      fixture.componentRef.setInput('plan', plan);
      component.disabled = false;
      fixture.detectChanges();

      let emitted: unknown;
      component.selectPlan.subscribe(p => (emitted = p));

      component.onSelectPlan();

      expect(emitted).toEqual(plan);
    });

    it('should NOT emit selectPlan when disabled', () => {
      fixture.componentRef.setInput('plan', fakePlan());
      component.disabled = true;
      fixture.detectChanges();

      let emitted = false;
      component.selectPlan.subscribe(() => (emitted = true));

      component.onSelectPlan();

      expect(emitted).toBeFalsy();
    });
  });
});
