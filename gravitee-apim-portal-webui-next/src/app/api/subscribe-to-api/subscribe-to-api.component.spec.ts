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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { By } from '@angular/platform-browser';

import { SubscribeToApiChoosePlanHarness } from './subscribe-to-api-choose-plan/subscribe-to-api-choose-plan.harness';
import { SubscribeToApiComponent } from './subscribe-to-api.component';
import { ApiAccessHarness } from '../../../components/api-access/api-access.harness';
import { Api } from '../../../entities/api/api';
import { fakeApi } from '../../../entities/api/api.fixtures';
import { fakePlan } from '../../../entities/plan/plan.fixture';
import { AppTestingModule, TESTING_BASE_URL } from '../../../testing/app-testing.module';

describe('SubscribeToApiComponent', () => {
  let component: SubscribeToApiComponent;
  let fixture: ComponentFixture<SubscribeToApiComponent>;
  let httpTestingController: HttpTestingController;
  let harnessLoader: HarnessLoader;

  const API_ID = 'api-id';
  const ENTRYPOINT = 'http://my.entrypoint';
  const KEYLESS_PLAN_ID = 'keyless-plan';
  const API_KEY_PLAN_ID = 'api-key-plan';
  const OAUTH2_PLAN_ID = 'oauth2-plan';
  const JWT_PLAN_ID = 'jwt-plan';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SubscribeToApiComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(SubscribeToApiComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);

    component = fixture.componentInstance;
    component.apiId = API_ID;
    fixture.detectChanges();

    httpTestingController.expectOne(`${TESTING_BASE_URL}/apis/${API_ID}/plans?size=-1`).flush({
      data: [
        fakePlan({ id: KEYLESS_PLAN_ID, security: 'KEY_LESS' }),
        fakePlan({ id: API_KEY_PLAN_ID, security: 'API_KEY' }),
        fakePlan({ id: OAUTH2_PLAN_ID, security: 'OAUTH2' }),
        fakePlan({ id: JWT_PLAN_ID, security: 'JWT' }),
      ],
    });
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('User subscribes to Keyless plan', () => {
    describe('Step 1 -- Choose a plan', () => {
      it('should be able to go to step 3 once plan chosen', async () => {
        const step1 = await harnessLoader.getHarness(SubscribeToApiChoosePlanHarness);
        expect(step1).toBeTruthy();

        expect(await step1.isPlanSelected(KEYLESS_PLAN_ID)).toEqual(false);
        expect(await canGoToNextStep()).toEqual(false);

        await step1.selectPlanByPlanId(KEYLESS_PLAN_ID);
        expect(await step1.isPlanSelected(KEYLESS_PLAN_ID)).toEqual(true);

        await goToNextStep();

        expectGetApi();
        fixture.detectChanges();
        expect(getTitle()).toEqual('Checkout');
      });
    });
    describe('Step 3 -- Checkout', () => {
      beforeEach(async () => {
        const step1 = await harnessLoader.getHarness(SubscribeToApiChoosePlanHarness);
        await step1.selectPlanByPlanId(KEYLESS_PLAN_ID);
        await goToNextStep();
        expectGetApi();
        fixture.detectChanges();
      });
      it('should see checkout information', async () => {
        expect(fixture.debugElement.query(By.css('app-subscription-info'))).toBeTruthy();
        const apiAccess = await harnessLoader.getHarness(ApiAccessHarness);
        expect(apiAccess).toBeTruthy();

        expect(await apiAccess.getBaseURL()).toEqual(ENTRYPOINT);
      });
      it('should not show subscribe button', async () => {
        expect(await getSubscribeButton()).toEqual(null);
      });
    });
  });

  describe('User subscribes to API Key plan', () => {
    describe('Step 1 -- Choose a plan', () => {
      it('should be disabled', async () => {
        const step1 = await harnessLoader.getHarness(SubscribeToApiChoosePlanHarness);
        expect(step1).toBeTruthy();

        expect(await step1.isPlanSelected(API_KEY_PLAN_ID)).toEqual(false);
        expect(await step1.isPlanDisabled(API_KEY_PLAN_ID)).toEqual(true);
        expect(await canGoToNextStep()).toEqual(false);
      });
    });
  });

  describe('User subscribes to OAuth2 plan', () => {
    describe('Step 1 -- Choose a plan', () => {
      it('should be disabled', async () => {
        const step1 = await harnessLoader.getHarness(SubscribeToApiChoosePlanHarness);
        expect(step1).toBeTruthy();

        expect(await step1.isPlanSelected(OAUTH2_PLAN_ID)).toEqual(false);
        expect(await step1.isPlanDisabled(OAUTH2_PLAN_ID)).toEqual(true);
        expect(await canGoToNextStep()).toEqual(false);
      });
    });
  });

  describe('User subscribes to JWT plan', () => {
    describe('Step 1 -- Choose a plan', () => {
      it('should be disabled', async () => {
        const step1 = await harnessLoader.getHarness(SubscribeToApiChoosePlanHarness);
        expect(step1).toBeTruthy();

        expect(await step1.isPlanSelected(JWT_PLAN_ID)).toEqual(false);
        expect(await step1.isPlanDisabled(JWT_PLAN_ID)).toEqual(true);
        expect(await canGoToNextStep()).toEqual(false);
      });
    });
  });

  function expectGetApi(api?: Api) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/apis/${API_ID}`).flush(api ?? fakeApi({ id: API_ID, entrypoints: [ENTRYPOINT] }));
  }

  async function canGoToNextStep(): Promise<boolean> {
    return await getNextStepButton().then(btn => !btn.isDisabled());
  }

  async function goToNextStep(): Promise<void> {
    return await getNextStepButton().then(btn => btn.click());
  }

  async function getNextStepButton(): Promise<MatButtonHarness> {
    return await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Next' }));
  }

  async function getSubscribeButton(): Promise<MatButtonHarness | null> {
    return await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: 'Subscribe' }));
  }

  function getTitle(): string {
    return fixture.debugElement.query(By.css('.m3-title-large')).nativeElement.textContent;
  }
});
