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
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { delay, of } from 'rxjs';

import { ApiProductPlanEditComponent } from './api-product-plan-edit.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../../../../entities/Constants';
import { fakePlanV4 } from '../../../../../entities/management-api-v2';

describe('ApiProductPlanEditComponent', () => {
    let fixture: ComponentFixture<ApiProductPlanEditComponent>;
    let loader: HarnessLoader;
    let httpTestingController: HttpTestingController;
    const apiProductId = 'my-product';

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [ApiProductPlanEditComponent, GioTestingModule, MatIconTestingModule, NoopAnimationsModule],
            providers: [
                { provide: Constants, useValue: CONSTANTS_TESTING },
                { provide: GioTestingPermissionProvider, useValue: ['environment-api_product_plan-u', 'environment-api_product_plan-r', 'environment-api_product_plan-c'] },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        snapshot: {
                            params: { apiProductId },
                            queryParams: { selectedPlanMenuItem: 'API_KEY' },
                        },
                        params: of({ apiProductId }).pipe(delay(0)),
                    },
                },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(ApiProductPlanEditComponent);
        loader = TestbedHarnessEnvironment.loader(fixture);
        httpTestingController = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        fixture.destroy();
        httpTestingController.verify();
    });

    it('should create a new plan', waitForAsync(async () => {
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges(false);
        await fixture.whenStable();
        handleBackgroundRequests();

        const saveBar = await loader.getHarness(GioSaveBarHarness);
        expect(await saveBar.isVisible()).toBe(true);

        const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
        await nameInput.setValue('New API Product Plan');
        fixture.detectChanges();

        // Create flow: Back & Next until the last step, then the Create button is shown
        let nextButton = await loader.getHarnessOrNull(MatButtonHarness.with({ selector: '[data-testid="api_product_plans_nextstep"]' }));
        while (nextButton) {
            await nextButton.click();
            await fixture.whenStable();
            await new Promise((r) => setTimeout(r, 150));
            fixture.detectChanges(false);
            nextButton = await loader.getHarnessOrNull(MatButtonHarness.with({ selector: '[data-testid="api_product_plans_nextstep"]' }));
        }

        await saveBar.clickSubmit();
        fixture.detectChanges();

        const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${apiProductId}/plans`);
        expect(req.request.method).toEqual('POST');
        expect(req.request.body.name).toEqual('New API Product Plan');
        expect(req.request.body.security.type).toEqual('API_KEY');
        req.flush(fakePlanV4());
        fixture.detectChanges();
    }));

    it('should edit an existing plan', waitForAsync(async () => {
        const planId = 'my-plan';
        (TestBed.inject(ActivatedRoute) as { snapshot: { params: Record<string, string> } }).snapshot.params = {
            apiProductId,
            planId,
        };

        fixture.detectChanges();
        await fixture.whenStable();

        const reqGet = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${apiProductId}/plans/${planId}`);
        reqGet.flush(fakePlanV4({ id: planId, name: 'Existing Plan', security: { type: 'API_KEY' } }));
        fixture.detectChanges(false);
        await fixture.whenStable();
        handleBackgroundRequests();
        fixture.detectChanges(false);

        const nameInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName="name"]' }));
        expect(await nameInput.getValue()).toBe('Existing Plan');

        await nameInput.setValue('Updated Plan');
        fixture.detectChanges();

        const saveBar = await loader.getHarness(GioSaveBarHarness);
        await saveBar.clickSubmit();
        fixture.detectChanges();

        const reqGetBeforePut = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${apiProductId}/plans/${planId}`);
        expect(reqGetBeforePut.request.method).toEqual('GET');
        reqGetBeforePut.flush(fakePlanV4({ id: planId, name: 'Existing Plan' }));

        const reqPut = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${apiProductId}/plans/${planId}`);
        expect(reqPut.request.method).toEqual('PUT');
        expect(reqPut.request.body.name).toEqual('Updated Plan');
        reqPut.flush(fakePlanV4());
        fixture.detectChanges();
    }));

    function handleBackgroundRequests() {
        const schemaReq = httpTestingController.match((req) => req.url.includes('api-key/schema'));
        schemaReq.forEach((r) => r.flush({}));
        const groupsReq = httpTestingController.match((req) => req.url.includes('configuration/groups') && !req.url.includes('_paged'));
        groupsReq.forEach((r) => r.flush([]));
    }
});


