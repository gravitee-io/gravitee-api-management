/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { HarnessLoader, parallel } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatButtonToggleHarness } from '@angular/material/button-toggle/testing';
import { MatMenuHarness } from '@angular/material/menu/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { PlanListComponent, PlanDS } from './plan-list.component';

import { PLAN_STATUS, PlanStatus, fakePlanV4 } from '../../../../../entities/management-api-v2';
import { PlanMenuItemVM } from '../../../../../services-ngx/constants.service';

const PLAN_MENU_ITEMS: PlanMenuItemVM[] = [
  { planFormType: 'API_KEY', name: 'API Key' },
  { planFormType: 'JWT', name: 'JWT' },
  { planFormType: 'MTLS', name: 'mTLS' },
];

describe('PlanListComponent', () => {
  let fixture: ComponentFixture<PlanListComponent>;
  let loader: HarnessLoader;

  function buildPlanStatuses(counts: Partial<Record<PlanStatus, number>> = {}) {
    return PLAN_STATUS.map(name => ({ name, number: counts[name] ?? 0 }));
  }

  async function create(
    inputs: Partial<{
      plans: PlanDS[];
      isReadOnly: boolean;
      showDeployOnColumn: boolean;
      canAddPlan: boolean;
      isV2Api: boolean;
    }> = {},
  ) {
    await TestBed.configureTestingModule({
      imports: [PlanListComponent, NoopAnimationsModule, RouterTestingModule, MatIconTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(PlanListComponent);
    fixture.componentRef.setInput('planMenuItems', PLAN_MENU_ITEMS);
    fixture.componentRef.setInput('planStatuses', buildPlanStatuses({ PUBLISHED: 1 }));
    fixture.componentRef.setInput('selectedStatus', 'PUBLISHED');
    fixture.componentRef.setInput('isLoadingData', false);
    fixture.componentRef.setInput('plans', inputs.plans ?? []);
    fixture.componentRef.setInput('isReadOnly', inputs.isReadOnly ?? false);
    fixture.componentRef.setInput('showDeployOnColumn', inputs.showDeployOnColumn ?? true);
    fixture.componentRef.setInput('canAddPlan', inputs.canAddPlan ?? true);
    fixture.componentRef.setInput('isV2Api', inputs.isV2Api ?? false);
    fixture.detectChanges();
    loader = TestbedHarnessEnvironment.loader(fixture);
  }

  async function getHeaderColumnNames(): Promise<Record<string, string>> {
    const table = await loader.getHarness(MatTableHarness.with({ selector: '#plansTable' }));
    const headerRows = await table.getHeaderRows();
    return headerRows[0].getCellTextByColumnName();
  }

  describe('column visibility', () => {
    it('shows Deploy on column when showDeployOnColumn is true', async () => {
      await create({ showDeployOnColumn: true });
      const headers = await getHeaderColumnNames();
      expect(headers['deploy-on']).toBe('Deploy on');
    });

    it('hides Deploy on column when showDeployOnColumn is false', async () => {
      await create({ showDeployOnColumn: false });
      const headers = await getHeaderColumnNames();
      expect(headers['deploy-on']).toBeUndefined();
    });

    it('shows drag handle column when not read-only', async () => {
      await create({ isReadOnly: false });
      const headers = await getHeaderColumnNames();
      expect(headers['drag-icon']).toBeDefined();
    });

    it('hides drag handle column when read-only', async () => {
      await create({ isReadOnly: true });
      const headers = await getHeaderColumnNames();
      expect(headers['drag-icon']).toBeUndefined();
    });
  });

  describe('empty state', () => {
    it('shows empty message when plans array is empty', async () => {
      await create({ plans: [] });
      const table = await loader.getHarness(MatTableHarness);
      expect(await (await table.host()).text()).toContain('There is no plan (yet).');
    });

    it('renders one row per plan when plans are provided', async () => {
      const plan = { ...fakePlanV4({ name: 'My JWT Plan', security: { type: 'JWT' } }), securityTypeLabel: 'JWT' } as PlanDS;
      await create({ plans: [plan] });
      const table = await loader.getHarness(MatTableHarness);
      const rows = await table.getRows();
      expect(rows).toHaveLength(1);
    });
  });

  describe('add new plan button', () => {
    it('shows add plan button when not read-only', async () => {
      await create({ isReadOnly: false, plans: [] });
      const btn = await loader.getHarnessOrNull(MatButtonHarness.with({ selector: '[aria-label="Add new plan"]' }));
      expect(btn).not.toBeNull();
    });

    it('hides add plan button when read-only', async () => {
      await create({ isReadOnly: true, plans: [] });
      const btn = await loader.getHarnessOrNull(MatButtonHarness.with({ selector: '[aria-label="Add new plan"]' }));
      expect(btn).toBeNull();
    });

    it('hides add plan button when canAddPlan is false', async () => {
      await create({ isReadOnly: false, canAddPlan: false, plans: [] });
      const btn = await loader.getHarnessOrNull(MatButtonHarness.with({ selector: '[aria-label="Add new plan"]' }));
      expect(btn).toBeNull();
    });

    it('add plan menu lists only the provided plan types', async () => {
      await create({ isReadOnly: false, plans: [] });
      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Add new plan"]' })).then(btn => btn.click());
      const menu = await loader.getHarness(MatMenuHarness);
      const items = await menu.getItems();
      const texts = await parallel(() => items.map(i => i.getText()));
      expect(texts).toEqual(['API Key', 'JWT', 'mTLS']);
    });
  });

  describe('status filter', () => {
    it('renders toggle buttons for PUBLISHED and STAGING statuses', async () => {
      await create({});
      const toggles = await loader.getAllHarnesses(MatButtonToggleHarness);
      const labels = await parallel(() => toggles.map(t => t.getText()));
      expect(labels.some(l => l.includes('PUBLISHED'))).toBe(true);
      expect(labels.some(l => l.includes('STAGING'))).toBe(true);
    });

    it('emits selected status when status toggle is clicked', async () => {
      await create({});
      const emitted: PlanStatus[] = [];
      fixture.componentInstance.statusFilterChanged.subscribe((s: PlanStatus) => emitted.push(s));
      await loader.getHarness(MatButtonToggleHarness.with({ text: /STAGING/ })).then(btn => btn.toggle());
      expect(emitted).toContain('STAGING');
    });
  });

  describe('plan type selection', () => {
    it('emits plan form type when menu item is clicked', async () => {
      await create({ isReadOnly: false, plans: [] });
      const emitted: string[] = [];
      fixture.componentInstance.planTypeSelected.subscribe((type: string) => emitted.push(type));
      await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Add new plan"]' })).then(btn => btn.click());
      const menu = await loader.getHarness(MatMenuHarness);
      await menu.clickItem({ text: 'JWT' });
      expect(emitted).toEqual(['JWT']);
    });
  });

  describe('design button visibility', () => {
    it('shows design button for V2 API when plan is not closed', async () => {
      const plan = {
        ...fakePlanV4({ name: 'JWT Plan', status: 'PUBLISHED', security: { type: 'JWT' } }),
        securityTypeLabel: 'JWT',
      } as PlanDS;
      await create({ plans: [plan], isReadOnly: false, isV2Api: true });
      const designBtn = await loader.getHarnessOrNull(MatButtonHarness.with({ selector: '[aria-label="Design the plan"]' }));
      expect(designBtn).not.toBeNull();
    });

    it('hides design button when isV2Api is false', async () => {
      const plan = {
        ...fakePlanV4({ name: 'JWT Plan', status: 'PUBLISHED', security: { type: 'JWT' } }),
        securityTypeLabel: 'JWT',
      } as PlanDS;
      await create({ plans: [plan], isReadOnly: false, isV2Api: false });
      const designBtn = await loader.getHarnessOrNull(MatButtonHarness.with({ selector: '[aria-label="Design the plan"]' }));
      expect(designBtn).toBeNull();
    });
  });
});
