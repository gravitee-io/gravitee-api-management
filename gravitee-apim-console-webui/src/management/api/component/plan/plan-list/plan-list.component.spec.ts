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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { PlanListComponent, PlanDS, type PlanFilterState, type PlanListContext } from './plan-list.component';
import { PlanListComponentHarness } from './plan-list.component.harness';

import { PLAN_STATUS, PlanStatus, fakePlanV4 } from '../../../../../entities/management-api-v2';
import { PlanMenuItemVM } from '../../../../../services-ngx/constants.service';

const PLAN_MENU_ITEMS: PlanMenuItemVM[] = [
  { planFormType: 'API_KEY', name: 'API Key' },
  { planFormType: 'JWT', name: 'JWT' },
  { planFormType: 'MTLS', name: 'mTLS' },
];

describe('PlanListComponent', () => {
  let fixture: ComponentFixture<PlanListComponent>;
  let harness: PlanListComponentHarness;

  function buildPlanStatuses(counts: Partial<Record<PlanStatus, number>> = {}) {
    return PLAN_STATUS.map(name => ({ name, number: counts[name] ?? 0 }));
  }

  function buildFilterState(overrides: Partial<PlanFilterState> = {}): PlanFilterState {
    return {
      statuses: buildPlanStatuses({ PUBLISHED: 1 }),
      selectedStatus: 'PUBLISHED',
      ...overrides,
    };
  }

  async function create(
    inputs: Partial<{
      plans: PlanDS[];
      context: PlanListContext;
      filterState: PlanFilterState;
    }> = {},
  ) {
    await TestBed.configureTestingModule({
      imports: [PlanListComponent, NoopAnimationsModule, MatIconTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(PlanListComponent);
    fixture.componentRef.setInput('planMenuItems', PLAN_MENU_ITEMS);
    fixture.componentRef.setInput('filterState', inputs.filterState ?? buildFilterState());
    fixture.componentRef.setInput('isLoadingData', false);
    fixture.componentRef.setInput('plans', inputs.plans ?? []);
    fixture.componentRef.setInput('context', inputs.context ?? {});
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, PlanListComponentHarness);
  }

  describe('column visibility', () => {
    it('shows Deploy on column when showDeployOnColumn is true', async () => {
      await create({ context: { showDeployOnColumn: true } });
      const headers = await harness.getHeaderColumnNames();
      expect(headers['deploy-on']).toBe('Deploy on');
    });

    it('hides Deploy on column when showDeployOnColumn is false', async () => {
      await create({ context: { showDeployOnColumn: false } });
      const headers = await harness.getHeaderColumnNames();
      expect(headers['deploy-on']).toBeUndefined();
    });

    it('shows drag handle column when not read-only', async () => {
      await create({ context: { isReadOnly: false } });
      const headers = await harness.getHeaderColumnNames();
      expect(headers['drag-icon']).toBeDefined();
    });

    it('hides drag handle column when read-only', async () => {
      await create({ context: { isReadOnly: true } });
      const headers = await harness.getHeaderColumnNames();
      expect(headers['drag-icon']).toBeUndefined();
    });
  });

  describe('empty state', () => {
    it('shows empty message when plans array is empty', async () => {
      await create({ plans: [] });
      expect(await harness.getTableText()).toContain('There is no plan (yet).');
    });

    it('renders one row per plan when plans are provided', async () => {
      const plan = { ...fakePlanV4({ name: 'My JWT Plan', security: { type: 'JWT' } }), securityTypeLabel: 'JWT' } as PlanDS;
      await create({ plans: [plan] });
      expect(await harness.getRowCount()).toBe(1);
    });
  });

  describe('add new plan button', () => {
    it('shows add plan button when not read-only', async () => {
      await create({ context: { isReadOnly: false }, plans: [] });
      expect(await harness.isAddPlanButtonVisible()).toBe(true);
    });

    it('hides add plan button when read-only', async () => {
      await create({ context: { isReadOnly: true }, plans: [] });
      expect(await harness.isAddPlanButtonVisible()).toBe(false);
    });

    it('hides add plan button when canAddPlan is false', async () => {
      await create({ context: { isReadOnly: false, canAddPlan: false }, plans: [] });
      expect(await harness.isAddPlanButtonVisible()).toBe(false);
    });

    it('add plan menu lists only the provided plan types', async () => {
      await create({ context: { isReadOnly: false }, plans: [] });
      const texts = await harness.getAddPlanMenuItems();
      expect(texts).toEqual(['API Key', 'JWT', 'mTLS']);
    });
  });

  describe('status filter', () => {
    it('renders toggle buttons for PUBLISHED and STAGING statuses', async () => {
      await create({});
      const toggles = await harness.getStatusFilterToggles();
      const labels = await Promise.all(toggles.map(t => t.getText()));
      expect(labels.some(l => l.includes('PUBLISHED'))).toBe(true);
      expect(labels.some(l => l.includes('STAGING'))).toBe(true);
    });

    it('emits selected status when status toggle is clicked', async () => {
      await create({});
      const emitted: PlanStatus[] = [];
      fixture.componentInstance.statusFilterChanged.subscribe((s: PlanStatus) => emitted.push(s));
      await harness.selectStatusFilter(/STAGING/);
      expect(emitted).toContain('STAGING');
    });
  });

  describe('plan type selection', () => {
    it('emits plan form type when menu item is clicked', async () => {
      await create({ context: { isReadOnly: false }, plans: [] });
      const emitted: string[] = [];
      fixture.componentInstance.typeSelected.subscribe((type: string) => emitted.push(type));
      await harness.clickAddPlanMenuItem('JWT');
      expect(emitted).toEqual(['JWT']);
    });
  });

  describe('planSelected output', () => {
    it('emits selected plan when plan name cell is clicked for non-closed plan', async () => {
      const plan = {
        ...fakePlanV4({ name: 'JWT Plan', status: 'PUBLISHED', security: { type: 'JWT' } }),
        securityTypeLabel: 'JWT',
      } as PlanDS;
      await create({ plans: [plan] });
      const emitted: PlanDS[] = [];
      fixture.componentInstance.planSelected.subscribe((p: PlanDS) => emitted.push(p));

      await harness.clickPlanName();

      expect(emitted).toHaveLength(1);
      expect(emitted[0].id).toBe(plan.id);
    });

    it('does not emit planSelected when clicking a closed plan name', async () => {
      const plan = {
        ...fakePlanV4({ name: 'Closed Plan', status: 'CLOSED', security: { type: 'JWT' } }),
        securityTypeLabel: 'JWT',
      } as PlanDS;
      await create({ plans: [plan] });
      const emitted: PlanDS[] = [];
      fixture.componentInstance.planSelected.subscribe((p: PlanDS) => emitted.push(p));

      await harness.clickPlanName();

      expect(emitted).toHaveLength(0);
    });

    it('emits selected plan when edit button is clicked', async () => {
      const plan = {
        ...fakePlanV4({ name: 'JWT Plan', status: 'PUBLISHED', security: { type: 'JWT' } }),
        securityTypeLabel: 'JWT',
      } as PlanDS;
      await create({ plans: [plan], context: { isReadOnly: false } });
      const emitted: PlanDS[] = [];
      fixture.componentInstance.planSelected.subscribe((p: PlanDS) => emitted.push(p));

      await harness.clickEditPlanButton();

      expect(emitted).toHaveLength(1);
      expect(emitted[0].id).toBe(plan.id);
    });

    it('emits selected plan when view button is clicked in read-only mode', async () => {
      const plan = {
        ...fakePlanV4({ name: 'JWT Plan', status: 'PUBLISHED', security: { type: 'JWT' } }),
        securityTypeLabel: 'JWT',
      } as PlanDS;
      await create({ plans: [plan], context: { isReadOnly: true } });
      const emitted: PlanDS[] = [];
      fixture.componentInstance.planSelected.subscribe((p: PlanDS) => emitted.push(p));

      await harness.clickViewPlanButton();

      expect(emitted).toHaveLength(1);
      expect(emitted[0].id).toBe(plan.id);
    });
  });

  describe('design button visibility', () => {
    it('shows design button for V2 API when plan is not closed', async () => {
      const plan = {
        ...fakePlanV4({ name: 'JWT Plan', status: 'PUBLISHED', security: { type: 'JWT' } }),
        securityTypeLabel: 'JWT',
      } as PlanDS;
      await create({ plans: [plan], context: { isReadOnly: false, isV2Api: true } });
      expect(await harness.isDesignPlanButtonVisible()).toBe(true);
    });

    it('hides design button when isV2Api is false', async () => {
      const plan = {
        ...fakePlanV4({ name: 'JWT Plan', status: 'PUBLISHED', security: { type: 'JWT' } }),
        securityTypeLabel: 'JWT',
      } as PlanDS;
      await create({ plans: [plan], context: { isReadOnly: false, isV2Api: false } });
      expect(await harness.isDesignPlanButtonVisible()).toBe(false);
    });
  });
});
