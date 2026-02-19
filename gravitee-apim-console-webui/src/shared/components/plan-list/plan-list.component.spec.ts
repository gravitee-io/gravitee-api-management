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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Component, signal } from '@angular/core';
import { CdkDragDrop } from '@angular/cdk/drag-drop';

import { PlanListComponent, PlanListTableRow } from './plan-list.component';

import { GioTestingModule } from '../../testing';
import { fakePlanV4 } from '../../../entities/management-api-v2';

describe('PlanListComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let loader: HarnessLoader;

  @Component({
    standalone: true,
    imports: [PlanListComponent],
    template: `
      <plan-list
        [dataSource]="dataSource()"
        [displayedColumns]="displayedColumns()"
        [isLoading]="isLoading()"
        [isReadOnly]="isReadOnly()"
        [dragEnabled]="dragEnabled()"
        [planDetailLink]="planDetailLink()"
        [showDesignButton]="showDesignButton()"
        [ariaLabel]="ariaLabel()"
        [emptyMessage]="emptyMessage()"
        [loadingMessage]="loadingMessage()"
        [tableId]="tableId()"
        [testIdPrefix]="testIdPrefix()"
        (dropRow)="dropRow($event)"
        (designPlan)="designPlan($event)"
        (publishPlan)="publishPlan($event)"
        (deprecatePlan)="deprecatePlan($event)"
        (closePlan)="closePlan($event)"
      />
    `,
  })
  class TestHostComponent {
    dataSource = signal<PlanListTableRow[]>([]);
    displayedColumns = signal<string[]>(['name', 'type', 'status', 'actions']);
    isLoading = signal(false);
    isReadOnly = signal(false);
    dragEnabled = signal(false);
    planDetailLink = signal<string | null>(null);
    showDesignButton = signal(false);
    ariaLabel = signal('Plans table');
    emptyMessage = signal('There is no plan (yet).');
    loadingMessage = signal('Loading...');
    tableId = signal<string | null>('testPlansTable');
    testIdPrefix = signal<string | null>('test_');

    dropRow(_event: CdkDragDrop<PlanListTableRow[]>) {}
    designPlan(_planId: string) {}
    publishPlan(_plan: PlanListTableRow) {}
    deprecatePlan(_plan: PlanListTableRow) {}
    closePlan(_plan: PlanListTableRow) {}
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent, GioTestingModule, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  afterEach(() => {
    jest.clearAllMocks();
    fixture.destroy();
  });

  it('should show empty message when not loading and dataSource is empty', async () => {
    fixture.componentInstance.dataSource.set([]);
    fixture.componentInstance.isLoading.set(false);
    fixture.detectChanges();

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#testPlansTable' }));
    const host = await table.host();
    expect(await host.text()).toContain('There is no plan (yet).');
  });

  it('should show loading message when loading', async () => {
    fixture.componentInstance.isLoading.set(true);
    fixture.detectChanges();

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#testPlansTable' }));
    const host = await table.host();
    expect(await host.text()).toContain('Loading...');
  });

  it('should display plan rows with name, type, status and security type label', async () => {
    const plans: PlanListTableRow[] = [
      {
        ...fakePlanV4({ name: 'Plan A', status: 'PUBLISHED', security: { type: 'API_KEY' } }),
        securityTypeLabel: 'API Key',
      },
      {
        ...fakePlanV4({ name: 'Plan B', status: 'STAGING', security: { type: 'JWT' } }),
        securityTypeLabel: 'JWT',
      },
    ];
    fixture.componentInstance.dataSource.set(plans);
    fixture.componentInstance.isLoading.set(false);
    fixture.detectChanges();

    const table = await loader.getHarness(MatTableHarness.with({ selector: '#testPlansTable' }));
    const rows = await table.getRows();
    expect(rows.length).toBe(2);

    const rowTexts = await Promise.all(rows.map(row => row.getCellTextByIndex()));
    expect(rowTexts[0][0]).toContain('Plan A');
    expect(rowTexts[0][1]).toContain('API Key');
    expect(rowTexts[0][2]).toContain('PUBLISHED');
    expect(rowTexts[1][0]).toContain('Plan B');
    expect(rowTexts[1][1]).toContain('JWT');
    expect(rowTexts[1][2]).toContain('STAGING');
  });

  it('should show Publish button only for STAGING plans when not read-only', async () => {
    const stagingPlan: PlanListTableRow = {
      ...fakePlanV4({ id: 'staging-1', name: 'Staging Plan', status: 'STAGING' }),
      securityTypeLabel: 'API Key',
    };
    fixture.componentInstance.dataSource.set([stagingPlan]);
    fixture.componentInstance.isReadOnly.set(false);
    fixture.componentInstance.isLoading.set(false);
    fixture.detectChanges();

    const publishBtn = await loader.getHarness(
      MatButtonHarness.with({ selector: '[data-testid="test_publish_plan_button"]' }),
    );
    expect(await publishBtn.isDisabled()).toBe(false);
  });

  it('should show Deprecate button only for PUBLISHED plans when not read-only', async () => {
    const publishedPlan: PlanListTableRow = {
      ...fakePlanV4({ id: 'pub-1', name: 'Published Plan', status: 'PUBLISHED' }),
      securityTypeLabel: 'API Key',
    };
    fixture.componentInstance.dataSource.set([publishedPlan]);
    fixture.componentInstance.isReadOnly.set(false);
    fixture.componentInstance.isLoading.set(false);
    fixture.detectChanges();

    const deprecateBtn = await loader.getHarness(
      MatButtonHarness.with({ selector: '[data-testid="test_deprecate_plan_button"]' }),
    );
    expect(deprecateBtn).toBeTruthy();
  });

  it('should emit publishPlan with plan when Publish button is clicked', async () => {
    const plan: PlanListTableRow = {
      ...fakePlanV4({ id: 'p1', name: 'Staging', status: 'STAGING' }),
      securityTypeLabel: 'API Key',
    };
    fixture.componentInstance.dataSource.set([plan]);
    fixture.componentInstance.isReadOnly.set(false);
    fixture.componentInstance.isLoading.set(false);
    fixture.detectChanges();

    const publishBtn = await loader.getHarness(
      MatButtonHarness.with({ selector: '[data-testid="test_publish_plan_button"]' }),
    );
    const spy = jest.spyOn(fixture.componentInstance, 'publishPlan');
    await publishBtn.click();

    expect(spy).toHaveBeenCalledWith(plan);
  });

  it('should emit closePlan when Close button is clicked', async () => {
    const plan: PlanListTableRow = {
      ...fakePlanV4({ id: 'p1', name: 'Published', status: 'PUBLISHED' }),
      securityTypeLabel: 'API Key',
    };
    fixture.componentInstance.dataSource.set([plan]);
    fixture.componentInstance.isReadOnly.set(false);
    fixture.componentInstance.isLoading.set(false);
    fixture.detectChanges();

    const closeBtn = await loader.getHarness(
      MatButtonHarness.with({ selector: '[data-testid="test_close_plan_button"]' }),
    );
    const spy = jest.spyOn(fixture.componentInstance, 'closePlan');
    await closeBtn.click();

    expect(spy).toHaveBeenCalledWith(plan);
  });

  it('should show View button when read-only instead of Edit', async () => {
    const plan: PlanListTableRow = {
      ...fakePlanV4({ id: 'p1', name: 'Plan', status: 'PUBLISHED' }),
      securityTypeLabel: 'API Key',
    };
    fixture.componentInstance.dataSource.set([plan]);
    fixture.componentInstance.isReadOnly.set(true);
    fixture.componentInstance.isLoading.set(false);
    fixture.detectChanges();

    const viewBtn = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="View the plan details"]' }));
    expect(viewBtn).toBeTruthy();
    const editBtn = await loader.getHarnessOrNull(MatButtonHarness.with({ selector: '[data-testid="test_edit_plan_button"]' }));
    expect(editBtn).toBeNull();
  });

  it('should not show Publish or Deprecate for CLOSED plan', async () => {
    const closedPlan: PlanListTableRow = {
      ...fakePlanV4({ id: 'c1', name: 'Closed', status: 'CLOSED' }),
      securityTypeLabel: 'API Key',
    };
    fixture.componentInstance.dataSource.set([closedPlan]);
    fixture.componentInstance.isReadOnly.set(false);
    fixture.componentInstance.isLoading.set(false);
    fixture.detectChanges();

    const publishBtn = await loader.getHarnessOrNull(
      MatButtonHarness.with({ selector: '[data-testid="test_publish_plan_button"]' }),
    );
    const deprecateBtn = await loader.getHarnessOrNull(
      MatButtonHarness.with({ selector: '[data-testid="test_deprecate_plan_button"]' }),
    );
    expect(publishBtn).toBeNull();
    expect(deprecateBtn).toBeNull();
  });
});
