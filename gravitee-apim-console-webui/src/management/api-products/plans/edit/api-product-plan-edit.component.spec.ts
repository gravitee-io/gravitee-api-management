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
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { of } from 'rxjs';

import { ApiProductPlanEditComponent } from './api-product-plan-edit.component';
import { ApiProductPlanEditComponentHarness } from './api-product-plan-edit.component.harness';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { fakePlanV4 } from '../../../../entities/management-api-v2';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { ApiProductV2Service } from '../../../../services-ngx/api-product-v2.service';

describe('ApiProductPlanEditComponent', () => {
  const API_PRODUCT_ID = 'product-xyz';
  const PLAN_ID = 'plan-123';

  let fixture: ComponentFixture<ApiProductPlanEditComponent>;
  let harness: ApiProductPlanEditComponentHarness;
  let httpTestingController: HttpTestingController;
  let routerNavigateSpy: jest.SpyInstance;
  let notifyApiProductChangedSpy: jest.SpyInstance;
  const snackBarService = { error: jest.fn(), success: jest.fn() };

  async function setup(
    params: { planId?: string; queryParams?: Record<string, string> } = {},
    permissions: string[] = ['api_product-plan-u', 'api_product-plan-r'],
  ) {
    const routeParams = { apiProductId: API_PRODUCT_ID, ...(params.planId ? { planId: params.planId } : {}) };
    const queryParams = params.queryParams ?? { selectedPlanMenuItem: 'API_KEY' };

    await TestBed.configureTestingModule({
      imports: [ApiProductPlanEditComponent, NoopAnimationsModule, GioTestingModule, MatIconTestingModule],
      providers: [
        { provide: GioTestingPermissionProvider, useValue: permissions },
        { provide: SnackBarService, useValue: snackBarService },
        {
          provide: InteractivityChecker,
          useValue: { isFocusable: () => true },
        },
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(convertToParamMap(routeParams)),
            queryParamMap: of(convertToParamMap(queryParams)),
            snapshot: { params: routeParams, queryParams },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiProductPlanEditComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    routerNavigateSpy = jest.spyOn(router, 'navigate');
    notifyApiProductChangedSpy = jest.spyOn(TestBed.inject(ApiProductV2Service), 'notifyApiProductChanged');
    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiProductPlanEditComponentHarness);
  }

  afterEach(() => {
    httpTestingController.verify();
    jest.clearAllMocks();
  });

  /** Submits the plan form by dispatching submit so ngSubmit runs. */
  function submitForm(): void {
    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
  }

  describe('create mode', () => {
    it('does not fetch plan in create mode and only loads policy schema for selected type', fakeAsync(async () => {
      await setup({ queryParams: { selectedPlanMenuItem: 'API_KEY' } });
      tick();
      fixture.detectChanges();

      httpTestingController.expectNone(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${PLAN_ID}`);
      // ApiPlanFormComponent fetches the policy schema for the selected plan type
      httpTestingController.match(`${CONSTANTS_TESTING.org.v2BaseURL}/plugins/policies/api-key/schema`).forEach(r => r.flush({}));
    }));

    it('creates plan on submit and navigates to list with STAGING filter', fakeAsync(async () => {
      await setup({ queryParams: { selectedPlanMenuItem: 'JWT' } });
      tick();
      fixture.detectChanges();

      await harness.setPlanName('My JWT Plan');
      await harness.clickNextStep();
      httpTestingController.match(`${CONSTANTS_TESTING.org.v2BaseURL}/plugins/policies/jwt/schema`).forEach(r => r.flush({}));
      tick();
      fixture.detectChanges();
      await harness.clickNextStep();
      tick();
      fixture.detectChanges();
      submitForm();
      tick();
      fixture.detectChanges();

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans`,
      });
      expect(req.request.body.definitionVersion).toBe('V4');
      expect(req.request.body.security?.type).toBe('JWT');
      req.flush(fakePlanV4({ id: 'new-plan', status: 'STAGING' }));
      tick();
      fixture.detectChanges();

      httpTestingController.match(`${CONSTANTS_TESTING.org.v2BaseURL}/plugins/policies/jwt/schema`).forEach(r => r.flush({}));

      expect(snackBarService.success).toHaveBeenCalledWith('Configuration successfully saved!');
      expect(routerNavigateSpy).toHaveBeenCalledWith(['../'], expect.objectContaining({ queryParams: { status: 'STAGING' } }));
    }));
  });

  describe('edit mode', () => {
    it('fetches plan by id in edit mode and does not send create request', fakeAsync(async () => {
      await setup({ planId: PLAN_ID });
      const plan = fakePlanV4({ id: PLAN_ID, status: 'PUBLISHED', security: { type: 'API_KEY' } });

      const getReq = httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${PLAN_ID}`);
      getReq.flush(plan);
      tick();
      fixture.detectChanges();
      httpTestingController.match(`${CONSTANTS_TESTING.org.v2BaseURL}/plugins/policies/api-key/schema`).forEach(r => r.flush({}));

      httpTestingController.expectNone({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans`,
      });
    }));

    it('loads plan in edit mode and shows save bar', fakeAsync(async () => {
      await setup({ planId: PLAN_ID });
      const plan = fakePlanV4({ id: PLAN_ID, name: 'Original', status: 'PUBLISHED', security: { type: 'API_KEY' } });

      httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${PLAN_ID}`).flush(plan);
      tick();
      fixture.detectChanges();
      httpTestingController.match(`${CONSTANTS_TESTING.org.v2BaseURL}/plugins/policies/api-key/schema`).forEach(r => r.flush({}));
      tick();
      fixture.detectChanges();

      expect(await harness.isSaveBarVisible()).toBe(true);
      const nameInput = await (await harness.getPlanForm()).getNameInput();
      expect(await nameInput.getValue()).toBe(plan.name);
    }));

    it('shows error and stays on page when loading plan returns 404', fakeAsync(async () => {
      await setup({ planId: PLAN_ID });
      httpTestingController
        .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${PLAN_ID}`)
        .flush({ message: 'Plan not found' }, { status: 404, statusText: 'Not Found' });
      tick();
      fixture.detectChanges();

      expect(snackBarService.error).toHaveBeenCalledWith('Plan not found');
      expect(routerNavigateSpy).not.toHaveBeenCalled();
    }));

    it('shows error and stays on page when update plan returns 400', fakeAsync(async () => {
      await setup({ planId: PLAN_ID });
      const plan = fakePlanV4({ id: PLAN_ID, name: 'Original', status: 'PUBLISHED', security: { type: 'API_KEY' } });

      httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${PLAN_ID}`).flush(plan);
      tick();
      fixture.detectChanges();
      httpTestingController.match(`${CONSTANTS_TESTING.org.v2BaseURL}/plugins/policies/api-key/schema`).forEach(r => r.flush({}));
      tick();
      fixture.detectChanges();

      await harness.setPlanName('Updated name');
      fixture.detectChanges();
      submitForm();
      tick();
      fixture.detectChanges();

      httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${PLAN_ID}`).flush(plan);
      tick();

      const putReq = httpTestingController.expectOne({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${PLAN_ID}`,
      });
      putReq.flush({ message: 'Validation failed' }, { status: 400, statusText: 'Bad Request' });
      tick();

      httpTestingController.match(`${CONSTANTS_TESTING.org.v2BaseURL}/plugins/policies/api-key/schema`).forEach(r => r.flush({}));

      expect(snackBarService.error).toHaveBeenCalledWith('Validation failed');
      expect(snackBarService.success).not.toHaveBeenCalled();
      expect(routerNavigateSpy).not.toHaveBeenCalled();
    }));
  });

  describe('form validation', () => {
    it('does not send save request when form is invalid', fakeAsync(async () => {
      await setup({ planId: PLAN_ID });
      const plan = fakePlanV4({ id: PLAN_ID, name: 'Original', status: 'PUBLISHED', security: { type: 'API_KEY' } });
      httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${PLAN_ID}`).flush(plan);
      tick();
      fixture.detectChanges();
      httpTestingController.match(`${CONSTANTS_TESTING.org.v2BaseURL}/plugins/policies/api-key/schema`).forEach(r => r.flush({}));
      tick();
      fixture.detectChanges();

      await harness.setPlanName('');
      fixture.detectChanges();
      submitForm();
      tick();
      httpTestingController
        .match(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${PLAN_ID}`)
        .forEach(req => req.error(new ProgressEvent('error')));

      httpTestingController.expectNone({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans`,
      });
      httpTestingController.expectNone({
        method: 'PUT',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${PLAN_ID}`,
      });
      expect(routerNavigateSpy).not.toHaveBeenCalled();
    }));
  });

  describe('create mode error handling', () => {
    it('shows error and stays on page when create plan returns 409', fakeAsync(async () => {
      await setup({ queryParams: { selectedPlanMenuItem: 'JWT' } });
      tick();
      fixture.detectChanges();

      await harness.setPlanName('My JWT Plan');
      await harness.clickNextStep();
      httpTestingController.match(`${CONSTANTS_TESTING.org.v2BaseURL}/plugins/policies/jwt/schema`).forEach(r => r.flush({}));
      tick();
      fixture.detectChanges();
      await harness.clickNextStep();
      tick();
      fixture.detectChanges();
      submitForm();
      tick();
      fixture.detectChanges();

      const req = httpTestingController.expectOne({
        method: 'POST',
        url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans`,
      });
      req.flush({ message: 'Plan name already exists' }, { status: 409, statusText: 'Conflict' });
      tick();

      httpTestingController.match(`${CONSTANTS_TESTING.org.v2BaseURL}/plugins/policies/jwt/schema`).forEach(r => r.flush({}));

      expect(snackBarService.error).toHaveBeenCalledWith('Plan name already exists');
      expect(snackBarService.success).not.toHaveBeenCalled();
      expect(routerNavigateSpy).not.toHaveBeenCalled();
    }));
  });

  describe('read-only mode', () => {
    it('hides save bar and disables form when user has no update permission', fakeAsync(async () => {
      await setup({ planId: PLAN_ID }, ['api_product-plan-r']);
      const plan = fakePlanV4({ id: PLAN_ID, status: 'PUBLISHED', security: { type: 'API_KEY' } });

      httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${PLAN_ID}`).flush(plan);
      tick();
      fixture.detectChanges();
      httpTestingController.match(`${CONSTANTS_TESTING.org.v2BaseURL}/plugins/policies/api-key/schema`).forEach(r => r.flush({}));
      tick();
      fixture.detectChanges();

      expect(await harness.isSaveBarVisible()).toBe(false);
    }));
  });

  describe('deploy banner notification', () => {
    it('notifies plan state changed after creating a plan so deploy banner is triggered', fakeAsync(async () => {
      await setup({ queryParams: { selectedPlanMenuItem: 'JWT' } });
      tick();
      fixture.detectChanges();
      httpTestingController.match(`${CONSTANTS_TESTING.org.v2BaseURL}/plugins/policies/jwt/schema`).forEach(r => r.flush({}));
      tick();
      fixture.detectChanges();

      const planValue = fakePlanV4({ name: 'New Plan', security: { type: 'JWT' }, status: 'STAGING' });
      fixture.componentInstance['planForm'].patchValue({ plan: planValue });
      jest.spyOn(fixture.componentInstance['planForm'], 'invalid', 'get').mockReturnValue(false);
      fixture.componentInstance['onSubmit']();
      tick();
      fixture.detectChanges();

      httpTestingController
        .expectOne({ method: 'POST', url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans` })
        .flush(fakePlanV4({ id: 'new-plan', status: 'STAGING' }));
      tick();
      fixture.detectChanges();

      expect(notifyApiProductChangedSpy).toHaveBeenCalledTimes(1);
    }));

    it('notifies plan state changed after updating a plan so deploy banner is triggered', fakeAsync(async () => {
      await setup({ planId: PLAN_ID });
      const plan = fakePlanV4({ id: PLAN_ID, name: 'Original', status: 'PUBLISHED', security: { type: 'API_KEY' } });

      httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${PLAN_ID}`).flush(plan);
      tick();
      fixture.detectChanges();

      jest.spyOn(fixture.componentInstance['planForm'], 'invalid', 'get').mockReturnValue(false);
      fixture.componentInstance['onSubmit']();
      tick();
      fixture.detectChanges();

      httpTestingController.expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${PLAN_ID}`).flush(plan);
      tick();

      httpTestingController
        .expectOne({ method: 'PUT', url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans/${PLAN_ID}` })
        .flush({ ...plan });
      tick();
      fixture.detectChanges();

      httpTestingController.match(`${CONSTANTS_TESTING.org.v2BaseURL}/plugins/policies/api-key/schema`).forEach(r => r.flush({}));

      expect(notifyApiProductChangedSpy).toHaveBeenCalledTimes(1);
    }));

    it('does not notify plan state changed when save fails', fakeAsync(async () => {
      await setup({ queryParams: { selectedPlanMenuItem: 'JWT' } });
      tick();
      fixture.detectChanges();
      httpTestingController.match(`${CONSTANTS_TESTING.org.v2BaseURL}/plugins/policies/jwt/schema`).forEach(r => r.flush({}));
      tick();
      fixture.detectChanges();

      const planValue = fakePlanV4({ name: 'New Plan', security: { type: 'JWT' }, status: 'STAGING' });
      fixture.componentInstance['planForm'].patchValue({ plan: planValue });
      jest.spyOn(fixture.componentInstance['planForm'], 'invalid', 'get').mockReturnValue(false);
      fixture.componentInstance['onSubmit']();
      tick();
      fixture.detectChanges();

      httpTestingController
        .expectOne({ method: 'POST', url: `${CONSTANTS_TESTING.env.v2BaseURL}/api-products/${API_PRODUCT_ID}/plans` })
        .flush({ message: 'Plan creation failed' }, { status: 500, statusText: 'Server Error' });
      tick();

      expect(notifyApiProductChangedSpy).not.toHaveBeenCalled();
    }));
  });
});
