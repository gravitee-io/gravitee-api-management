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
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { MatTableHarness } from '@angular/material/table/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatMenuHarness } from '@angular/material/menu/testing';
import { MatButtonToggleHarness } from '@angular/material/button-toggle/testing';

import { ApiProductPlanListComponent } from './api-product-plan-list.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../../../entities/Constants';
import { fakePlanV4 } from '../../../../../entities/management-api-v2';

describe('ApiProductPlanListComponent', () => {
    let fixture: ComponentFixture<ApiProductPlanListComponent>;
    let loader: HarnessLoader;
    let httpTestingController: HttpTestingController;
    const apiProductId = 'my-product';

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [ApiProductPlanListComponent, GioTestingModule, MatIconTestingModule, NoopAnimationsModule],
            providers: [
                { provide: Constants, useValue: CONSTANTS_TESTING },
                { provide: GioTestingPermissionProvider, useValue: ['environment-api_product_plan-u', 'environment-api_product_plan-r'] },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        snapshot: {
                            params: { apiProductId },
                            queryParams: { status: 'PUBLISHED' },
                        },
                    },
                },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(ApiProductPlanListComponent);
        loader = TestbedHarnessEnvironment.loader(fixture);
        httpTestingController = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        fixture.destroy();
        httpTestingController.verify();
    });

    it('should display an empty table', fakeAsync(async () => {
        await initComponent([]);

        const table = await loader.getHarness(MatTableHarness.with({ selector: '#apiProductPlansTable' }));
        const rows = await table.getRows();
        expect(rows.length).toBe(0);

        expect(fixture.nativeElement.textContent).toContain('There is no plan (yet).');
    }));

    it('should display a table with plans', fakeAsync(async () => {
        const plans = [
            fakePlanV4({ name: 'Plan 1', status: 'PUBLISHED', security: { type: 'API_KEY' } }),
            fakePlanV4({ name: 'Plan 2', status: 'PUBLISHED', security: { type: 'JWT' } }),
        ];
        await initComponent(plans);

        const table = await loader.getHarness(MatTableHarness.with({ selector: '#apiProductPlansTable' }));
        const rows = await table.getRows();
        expect(rows.length).toBe(2);

        const rowConfigs = await Promise.all(rows.map(row => row.getCellTextByIndex()));
        // Columns: drag-icon, name, type, status, actions
        expect(rowConfigs[0][1]).toContain('Plan 1');
        expect(rowConfigs[0][2]).toContain('API Key');
        expect(rowConfigs[1][1]).toContain('Plan 2');
        expect(rowConfigs[1][2]).toContain('JWT');
    }));

    it('should show only API Key, JWT, mTLS in plan type menu', fakeAsync(async () => {
        await initComponent([]);

        const addButton = await loader.getHarness(MatButtonHarness.with({ selector: '[data-testid="api_product_plans_add_plan_button"]' }));
        await addButton.click();

        const menu = await loader.getHarness(MatMenuHarness);
        const items = await menu.getItems();
        const itemLabels = await Promise.all(items.map(item => item.getText()));

        expect(itemLabels).toEqual(['mTLS', 'JWT', 'API Key']);
    }));

    it('should filter by status', fakeAsync(async () => {
        await initComponent([
            fakePlanV4({ name: 'Published Plan', status: 'PUBLISHED' }),
            fakePlanV4({ name: 'Staging Plan', status: 'STAGING' }),
        ]);

        // Initial load for PUBLISHED (from query params)
        expect(fixture.componentInstance.plansTableDS().length).toBe(1);
        expect(fixture.componentInstance.plansTableDS()[0].name).toBe('Published Plan');

        // Filter by STAGING
        const stagingButton = await loader.getHarness(MatButtonToggleHarness.with({ text: /STAGING/ }));
        await stagingButton.check();

        const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${apiProductId}/plans?page=1&perPage=10&statuses=STAGING`);
        req.flush({ data: [fakePlanV4({ name: 'Staging Plan', status: 'STAGING' })] });
        tick();

        expect(fixture.componentInstance.plansTableDS().length).toBe(1);
        expect(fixture.componentInstance.plansTableDS()[0].name).toBe('Staging Plan');
    }));

    it('should call API to update plan order when row is dropped', fakeAsync(async () => {
        const plan1 = fakePlanV4({ id: 'plan-1', name: 'First', order: 1 });
        const plan2 = fakePlanV4({ id: 'plan-2', name: 'Second', order: 2 });
        await initComponent([plan1, plan2]);

        fixture.componentInstance.dropRow({
            previousIndex: 1,
            currentIndex: 0,
        } as any);

        const putReq = httpTestingController.expectOne(
            `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${apiProductId}/plans/plan-2`,
            'PUT',
        );
        expect(putReq.request.body.order).toBe(1);
        putReq.flush({ ...plan2, order: 1 });
        tick();

        const listReq = httpTestingController.expectOne(
            (req) => req.url.includes(`/api-products/${apiProductId}/plans`) && req.params.get('statuses')?.includes('PUBLISHED'),
        );
        listReq.flush({ data: [{ ...plan2, order: 1 }, { ...plan1, order: 2 }] });
        tick();
    }));

    async function initComponent(plans: any[]) {
        fixture.detectChanges();
        tick();

        const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${apiProductId}/plans?page=1&perPage=10&statuses=STAGING,PUBLISHED,DEPRECATED,CLOSED`);
        expect(req.request.method).toEqual('GET');
        req.flush({ data: plans });
        tick();
        fixture.detectChanges();
    }
});
