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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';

import { ApiPayloadMaskingRulesComponent } from './api-payload-masking-rules.component';
import { PayloadMaskingRuleDialogComponent } from './payload-masking-rule-dialog/payload-masking-rule-dialog.component';

import { fakeProxyApiV4, PayloadMaskingRule } from '../../../../../entities/management-api-v2';

describe('ApiPayloadMaskingRulesComponent', () => {
  let fixture: ComponentFixture<ApiPayloadMaskingRulesComponent>;
  let component: ApiPayloadMaskingRulesComponent;
  const mockDialog = { open: jest.fn() };

  const baseApi = fakeProxyApiV4({
    analytics: {
      enabled: true,
      tracing: {
        enabled: true,
        verbose: false,
        payloadMasking: {
          rules: [
            { path: '$.password', maskingStrategy: { type: 'FULL' }, phase: 'BOTH', format: 'JSON' },
            {
              path: '$.creditCard',
              maskingStrategy: { type: 'PARTIAL', prefixLength: 0, suffixLength: 4, replacement: '*' },
              phase: 'REQUEST',
              format: 'JSON',
            },
          ],
        },
      },
    },
  });

  async function setup(api = baseApi, resetTrigger = 0) {
    await TestBed.configureTestingModule({
      imports: [ApiPayloadMaskingRulesComponent, NoopAnimationsModule, MatIconTestingModule],
      providers: [{ provide: MatDialog, useValue: mockDialog }],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiPayloadMaskingRulesComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('api', api);
    fixture.componentRef.setInput('resetTrigger', resetTrigger);
    fixture.detectChanges();
  }

  afterEach(() => jest.clearAllMocks());

  describe('initialisation', () => {
    it('should display rows from existing API payload masking rules', async () => {
      await setup();

      const rows = component['rows']();
      expect(rows).toHaveLength(2);
      expect(rows[0].path).toBe('$.password');
      expect(rows[1].path).toBe('$.creditCard');
    });

    it('should display empty table when API has no payload masking rules', async () => {
      await setup(fakeProxyApiV4({ analytics: { enabled: true, tracing: { enabled: true, verbose: false } } }));

      expect(component['rows']()).toHaveLength(0);
    });

    it('should mark as read-only for Kubernetes-origin APIs', async () => {
      await setup(fakeProxyApiV4({ definitionContext: { origin: 'KUBERNETES' } }));

      expect(component['isReadOnly']()).toBe(true);
    });

    it('should not be read-only for management-origin APIs', async () => {
      await setup(baseApi);

      expect(component['isReadOnly']()).toBe(false);
    });
  });

  describe('addRule', () => {
    it('should add a row and emit rulesChange when dialog confirms', async () => {
      await setup();
      const emitted: PayloadMaskingRule[][] = [];
      component.rulesChange.subscribe(rules => emitted.push(rules));

      const newRule: PayloadMaskingRule = { path: '$.token', maskingStrategy: { type: 'FULL' }, phase: 'BOTH', format: 'JSON' };
      mockDialog.open.mockReturnValue({ afterClosed: () => of(newRule) });

      component['addRule']();
      fixture.detectChanges();

      expect(component['rows']()).toHaveLength(3);
      expect(component['rows']()[2].path).toBe('$.token');
      expect(emitted).toHaveLength(1);
      expect(emitted[0]).toHaveLength(3);
    });

    it('should not change rows when dialog is cancelled', async () => {
      await setup();
      const emitted: PayloadMaskingRule[][] = [];
      component.rulesChange.subscribe(rules => emitted.push(rules));

      mockDialog.open.mockReturnValue({ afterClosed: () => of(undefined) });

      component['addRule']();
      fixture.detectChanges();

      expect(component['rows']()).toHaveLength(2);
      expect(emitted).toHaveLength(0);
    });

    it('should open dialog with PayloadMaskingRuleDialogComponent', async () => {
      await setup();
      mockDialog.open.mockReturnValue({ afterClosed: () => of(undefined) });

      component['addRule']();

      expect(mockDialog.open).toHaveBeenCalledWith(PayloadMaskingRuleDialogComponent, expect.objectContaining({ data: { rule: null } }));
    });
  });

  describe('removeRule', () => {
    it('should remove the row and emit rulesChange', async () => {
      await setup();
      const emitted: PayloadMaskingRule[][] = [];
      component.rulesChange.subscribe(rules => emitted.push(rules));

      const idToRemove = component['rows']()[0]._id;
      component['removeRule'](idToRemove);
      fixture.detectChanges();

      expect(component['rows']()).toHaveLength(1);
      expect(component['rows']()[0].path).toBe('$.creditCard');
      expect(emitted).toHaveLength(1);
      expect(emitted[0]).toHaveLength(1);
    });
  });

  describe('editRule', () => {
    it('should update the row in place and emit rulesChange when dialog confirms', async () => {
      await setup();
      const emitted: PayloadMaskingRule[][] = [];
      component.rulesChange.subscribe(rules => emitted.push(rules));

      const idToEdit = component['rows']()[0]._id;
      const updatedRule: PayloadMaskingRule = {
        path: '$.password',
        maskingStrategy: { type: 'FULL', replacement: '[SECRET]' },
        phase: 'REQUEST',
        format: 'JSON',
      };
      mockDialog.open.mockReturnValue({ afterClosed: () => of(updatedRule) });

      component['editRule'](idToEdit);
      fixture.detectChanges();

      expect(component['rows']()).toHaveLength(2);
      expect(component['rows']()[0]._id).toBe(idToEdit);
      expect(component['rows']()[0].maskingStrategy?.replacement).toBe('[SECRET]');
      expect(emitted).toHaveLength(1);
    });
  });

  describe('resetTrigger', () => {
    it('should reset rows to API state when resetTrigger increments', async () => {
      await setup(baseApi, 0);

      component['rows'].update(rows => [...rows, { ...rows[0], _id: 'new', path: '$.extra' }]);
      expect(component['rows']()).toHaveLength(3);

      fixture.componentRef.setInput('resetTrigger', 1);
      fixture.detectChanges();

      expect(component['rows']()).toHaveLength(2);
      expect(component['rows']()[0].path).toBe('$.password');
    });
  });

  describe('emitted rules strip display-only fields', () => {
    it('should not include _id in emitted rules', async () => {
      await setup();
      const emitted: PayloadMaskingRule[][] = [];
      component.rulesChange.subscribe(rules => emitted.push(rules));

      const idToRemove = component['rows']()[1]._id;
      component['removeRule'](idToRemove);

      const emittedRule = emitted[0][0] as any;
      expect(emittedRule._id).toBeUndefined();
      expect(emittedRule.path).toBe('$.password');
    });
  });
});
