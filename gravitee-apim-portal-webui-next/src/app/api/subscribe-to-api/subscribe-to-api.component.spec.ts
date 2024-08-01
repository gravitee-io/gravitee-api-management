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

import { TermsAndConditionsDialogHarness } from './components/terms-and-conditions-dialog/terms-and-conditions-dialog.harness';
import { SubscribeToApiCheckoutHarness } from './subscribe-to-api-checkout/subscribe-to-api-checkout.harness';
import { SubscribeToApiChooseApplicationHarness } from './subscribe-to-api-choose-application/subscribe-to-api-choose-application.harness';
import { SubscribeToApiChoosePlanHarness } from './subscribe-to-api-choose-plan/subscribe-to-api-choose-plan.harness';
import { SubscribeToApiComponent } from './subscribe-to-api.component';
import { ApiAccessHarness } from '../../../components/api-access/api-access.harness';
import { RadioCardHarness } from '../../../components/radio-card/radio-card.harness';
import { Api } from '../../../entities/api/api';
import { fakeApi } from '../../../entities/api/api.fixtures';
import { ApplicationsResponse } from '../../../entities/application/application';
import { fakeApplication, fakeApplicationsResponse } from '../../../entities/application/application.fixture';
import { Page } from '../../../entities/page/page';
import { fakePage } from '../../../entities/page/page.fixtures';
import { fakePlan } from '../../../entities/plan/plan.fixture';
import { CreateSubscription, Subscription } from '../../../entities/subscription/subscription';
import { fakeSubscription, fakeSubscriptionResponse } from '../../../entities/subscription/subscription.fixture';
import { SubscriptionsResponse } from '../../../entities/subscription/subscriptions-response';
import { ConfigService } from '../../../services/config.service';
import { AppTestingModule, TESTING_BASE_URL } from '../../../testing/app-testing.module';

describe('SubscribeToApiComponent', () => {
  let component: SubscribeToApiComponent;
  let fixture: ComponentFixture<SubscribeToApiComponent>;
  let httpTestingController: HttpTestingController;
  let harnessLoader: HarnessLoader;
  let rootHarnessLoader: HarnessLoader;

  const API_ID = 'api-id';
  const ENTRYPOINT = 'http://my.entrypoint';
  const KEYLESS_PLAN_ID = 'keyless-plan';
  const API_KEY_PLAN_ID = 'api-key-plan';
  const API_KEY_PLAN_ID_COMMENT_REQUIRED = 'api-key-plan-comment-required';
  const API_KEY_PLAN_ID_GENERAL_CONDITIONS = 'api-key-plan-general-conditions';
  const OAUTH2_PLAN_ID = 'oauth2-plan';
  const JWT_PLAN_ID = 'jwt-plan';
  const GENERAL_CONDITIONS_ID = 'page-id';
  const APP_ID = 'app-id';
  const APP_ID_NO_SUBSCRIPTIONS = 'app-id-no-subscriptions';
  const APP_ID_ONE_API_KEY_SUBSCRIPTION = 'app-id-one-api-key-subscription';

  const init = async (sharedApiKeyModeEnabled: boolean) => {
    await TestBed.configureTestingModule({
      imports: [SubscribeToApiComponent, AppTestingModule],
      providers: [
        {
          provide: ConfigService,
          useValue: {
            baseURL: TESTING_BASE_URL,
            configuration: {
              plan: {
                security: {
                  sharedApiKey: {
                    enabled: sharedApiKeyModeEnabled,
                  },
                },
              },
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SubscribeToApiComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    rootHarnessLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    component = fixture.componentInstance;
    component.apiId = API_ID;
    fixture.detectChanges();

    httpTestingController.expectOne(`${TESTING_BASE_URL}/apis/${API_ID}/plans?size=-1`).flush({
      data: [
        fakePlan({ id: KEYLESS_PLAN_ID, security: 'KEY_LESS' }),
        fakePlan({ id: API_KEY_PLAN_ID, security: 'API_KEY', comment_required: false, general_conditions: undefined }),
        fakePlan({ id: API_KEY_PLAN_ID_COMMENT_REQUIRED, security: 'API_KEY', comment_required: true, general_conditions: undefined }),
        fakePlan({ id: API_KEY_PLAN_ID_GENERAL_CONDITIONS, security: 'API_KEY', general_conditions: GENERAL_CONDITIONS_ID }),
        fakePlan({ id: OAUTH2_PLAN_ID, security: 'OAUTH2' }),
        fakePlan({ id: JWT_PLAN_ID, security: 'JWT' }),
      ],
    });
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('User subscribes to Keyless plan', () => {
    beforeEach(async () => {
      await init(true);
    });
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
      beforeEach(async () => {
        await init(true);
      });

      it('should choose API Key plan and go to step 2', async () => {
        const step1 = await harnessLoader.getHarness(SubscribeToApiChoosePlanHarness);
        expect(step1).toBeTruthy();

        expect(await step1.isPlanSelected(API_KEY_PLAN_ID)).toEqual(false);
        expect(await step1.isPlanDisabled(API_KEY_PLAN_ID)).toEqual(false);

        await step1.selectPlanByPlanId(API_KEY_PLAN_ID);
        expect(await step1.isPlanSelected(API_KEY_PLAN_ID)).toEqual(true);

        await goToNextStep();

        expectGetSubscriptionsForApi(API_ID);
        expectGetApplications();
        fixture.detectChanges();

        expect(getTitle()).toEqual('Choose an application');
      });
    });
    describe('Step 2 -- Choose an application', () => {
      beforeEach(async () => {
        await init(true);

        const step1 = await harnessLoader.getHarness(SubscribeToApiChoosePlanHarness);
        await step1.selectPlanByPlanId(API_KEY_PLAN_ID);
        await goToNextStep();
      });

      describe('When user has no applications', () => {
        beforeEach(async () => {
          expectGetSubscriptionsForApi(API_ID);
          expectGetApplications(1, fakeApplicationsResponse({ data: [] }));
          fixture.detectChanges();
        });

        it('should not allow checkout', async () => {
          const step2 = await harnessLoader.getHarness(SubscribeToApiChooseApplicationHarness);
          expect(step2).toBeTruthy();
          expect(await step2.noApplicationsMessageShown()).toEqual(true);

          expect(await canGoToNextStep()).toEqual(false);
        });
      });

      describe('When user has applications', () => {
        const APP_ID_1 = 'app-id-1';
        const APP_ID_2 = 'app-id-2';
        beforeEach(async () => {
          expectGetSubscriptionsForApi(
            API_ID,
            fakeSubscriptionResponse({
              data: [
                fakeSubscription({ status: 'CLOSED', plan: API_KEY_PLAN_ID, application: '3' }),
                fakeSubscription({ status: 'ACCEPTED', plan: API_KEY_PLAN_ID, application: '4' }),
                fakeSubscription({ status: 'PENDING', plan: API_KEY_PLAN_ID, application: '5' }),
                fakeSubscription({ status: 'PENDING', plan: API_KEY_PLAN_ID_GENERAL_CONDITIONS, application: '6' }),
                fakeSubscription({ status: 'ACCEPTED', plan: API_KEY_PLAN_ID, application: '7' }),
                fakeSubscription({ status: 'PENDING', plan: API_KEY_PLAN_ID, application: '8' }),
              ],
              metadata: {
                [API_KEY_PLAN_ID]: {
                  planMode: 'STANDARD',
                  securityType: 'API_KEY',
                },
                [API_KEY_PLAN_ID_GENERAL_CONDITIONS]: {
                  planMode: 'STANDARD',
                  securityType: 'API_KEY',
                },
              },
            }),
          );
          expectGetApplications(
            1,
            fakeApplicationsResponse({
              data: [
                fakeApplication({ id: APP_ID_1, name: 'App 1' }),
                fakeApplication({ id: APP_ID_2, name: 'App 2' }),
                fakeApplication({ id: '3', name: 'App 3' }),
                fakeApplication({ id: '4', name: 'App 4' }),
                fakeApplication({ id: '5', name: 'App 5' }),
                fakeApplication({ id: '6', name: 'App 6', api_key_mode: 'SHARED' }),
                fakeApplication({ id: '7', name: 'App 7', api_key_mode: 'SHARED' }),
                fakeApplication({ id: '8', name: 'App 8', api_key_mode: 'SHARED' }),
                fakeApplication({ id: '9', name: 'App 9' }),
              ],
            }),
          );
          fixture.detectChanges();
        });

        it('should list applications and go to next step', async () => {
          const applications = await harnessLoader.getAllHarnesses(RadioCardHarness);
          expect(applications.length).toEqual(9);

          expect(await canGoToNextStep()).toEqual(false);

          const app1 = await harnessLoader.getHarnessOrNull(RadioCardHarness.with({ title: 'App 1' }));
          expect(app1).toBeTruthy();
          expect(await app1?.isDisabled()).toEqual(false);
          expect(await app1?.isSelected()).toEqual(false);

          const app2 = await harnessLoader.getHarnessOrNull(RadioCardHarness.with({ title: 'App 2' }));
          expect(await app2?.isDisabled()).toEqual(false);

          await app1?.select();

          expect(await app1?.isSelected()).toEqual(true);

          await goToNextStep();

          expectGetApi();
          fixture.detectChanges();
          expect(getTitle()).toEqual('Checkout');
        });

        it('should list pages of applications and go to next step', async () => {
          const step2 = await harnessLoader.getHarness(SubscribeToApiChooseApplicationHarness);
          expect(await step2.hasPreviousPageOfApplications()).toEqual(false);
          expect(await step2.hasNextPageOfApplications()).toEqual(true);

          await step2.getNextPageOfApplications();

          expectGetApplications(
            2,
            fakeApplicationsResponse({
              data: [fakeApplication({ id: '10', name: 'App 10' })],
              metadata: {
                pagination: {
                  total: 10,
                  size: 1,
                  first: 10,
                  last: 10,
                  current_page: 2,
                  total_pages: 2,
                },
              },
            }),
          );

          expect(await step2.hasPreviousPageOfApplications()).toEqual(true);
          expect(await step2.hasNextPageOfApplications()).toEqual(false);

          const app10 = await harnessLoader.getHarness(RadioCardHarness.with({ title: 'App 10' }));
          await app10.select();

          await step2.getPreviousPageOfApplications();
          expectGetApplications(1, fakeApplicationsResponse({ data: [] }));

          expect(getSelectedApplicationName()).toEqual('App 10');
          await goToNextStep();

          expectGetApi();
          fixture.detectChanges();
          expect(getTitle()).toEqual('Checkout');
        });

        it('should disable applications with valid subscriptions', async () => {
          const app3 = await harnessLoader.getHarness(RadioCardHarness.with({ title: 'App 3' }));
          expect(await app3.isDisabled()).toEqual(false);

          const app4 = await harnessLoader.getHarness(RadioCardHarness.with({ title: 'App 4' }));
          expect(await app4.isDisabled()).toEqual(true);

          const app5 = await harnessLoader.getHarness(RadioCardHarness.with({ title: 'App 5' }));
          expect(await app5.isDisabled()).toEqual(true);
        });

        it('should disable applications in Shared API Key mode that have valid subscriptions', async () => {
          const app3 = await harnessLoader.getHarness(RadioCardHarness.with({ title: 'App 6' }));
          expect(await app3.isDisabled()).toEqual(true);

          const app4 = await harnessLoader.getHarness(RadioCardHarness.with({ title: 'App 7' }));
          expect(await app4.isDisabled()).toEqual(true);

          const app5 = await harnessLoader.getHarness(RadioCardHarness.with({ title: 'App 8' }));
          expect(await app5.isDisabled()).toEqual(true);
        });
      });
    });

    describe('Step 3 -- Checkout', () => {
      describe('When comment is required', () => {
        beforeEach(async () => {
          await init(true);
          await selectPlan(API_KEY_PLAN_ID_COMMENT_REQUIRED);
          await selectApplication();

          expectGetApi();
          fixture.detectChanges();
        });
        it('should not allow subscribe without comment', async () => {
          const subscribeButton = await getSubscribeButton();
          expect(subscribeButton).toBeTruthy();
          expect(await subscribeButton?.isDisabled()).toEqual(true);
        });
        it('should subscribe with comment', async () => {
          const subscribeButton = await getSubscribeButton();
          expect(await subscribeButton?.isDisabled()).toEqual(true);

          const step3 = await harnessLoader.getHarness(SubscribeToApiCheckoutHarness);
          const messageBox = await step3.getMessageInput();
          await messageBox.setValue('My new message');

          expect(await subscribeButton?.isDisabled()).toEqual(false);
          await subscribeButton?.click();

          expectPostCreateSubscription({ plan: API_KEY_PLAN_ID_COMMENT_REQUIRED, application: 'app-id', request: 'My new message' });
        });
      });
      describe('When terms and conditions need to be accepted', () => {
        const PAGE = fakePage({
          id: GENERAL_CONDITIONS_ID,
          content: 'cats rule',
          type: 'MARKDOWN',
          contentRevisionId: { revision: 2, pageId: GENERAL_CONDITIONS_ID },
        });
        beforeEach(async () => {
          await init(true);
          await selectPlan(API_KEY_PLAN_ID_GENERAL_CONDITIONS);
          await selectApplication();

          expectGetApi();
          fixture.detectChanges();
        });
        it('should not allow subscribe without accepting terms and conditions', async () => {
          const subscribeButton = await getSubscribeButton();
          expect(subscribeButton).toBeTruthy();
          expect(await subscribeButton?.isDisabled()).toEqual(false);
          await subscribeButton?.click();

          expectGetPage(PAGE);

          const termsAndConditionsDialog = await rootHarnessLoader.getHarness(TermsAndConditionsDialogHarness);
          expect(termsAndConditionsDialog).toBeTruthy();

          const pageContent = await termsAndConditionsDialog.getMarkdownTermsAndConditions();
          expect(pageContent.getMarkdownHtml()).toContain(PAGE.content);

          await termsAndConditionsDialog.close();
        });
        it('should subscribe after accepting terms and conditions', async () => {
          const subscribeButton = await getSubscribeButton();
          await subscribeButton?.click();

          expectGetPage(PAGE);

          const termsAndConditionsDialog = await rootHarnessLoader.getHarness(TermsAndConditionsDialogHarness);
          await termsAndConditionsDialog.accept();

          expectPostCreateSubscription({
            plan: API_KEY_PLAN_ID_GENERAL_CONDITIONS,
            application: 'app-id',
            general_conditions_accepted: true,
            general_conditions_content_revision: PAGE.contentRevisionId,
          });
        });
      });
      describe('API Key Management', () => {
        describe('When a chosen application has no existing subscriptions', () => {
          beforeEach(async () => {
            await init(true);
            await selectPlan(API_KEY_PLAN_ID);
            await selectApplication(APP_ID_NO_SUBSCRIPTIONS);

            expectGetApi();
            expectGetSubscriptionsForApplication(APP_ID_NO_SUBSCRIPTIONS, fakeSubscriptionResponse({ data: [], metadata: {} }));
            fixture.detectChanges();
          });
          it('should NOT show api key mode choice', async () => {
            const step3 = await harnessLoader.getHarness(SubscribeToApiCheckoutHarness);
            expect(await step3.isChooseApiKeyModeVisible()).toBeFalsy();
          });
          it('should create subscription', async () => {
            const subscribe = await getSubscribeButton();
            await subscribe?.click();

            expectPostCreateSubscription({
              plan: API_KEY_PLAN_ID,
              application: APP_ID_NO_SUBSCRIPTIONS,
            });
          });
        });
        describe('When a chosen application has one existing API Key subscription', () => {
          describe('When the existing API Key subscription is with current API', () => {
            describe('When Shared API Key mode is enabled', () => {
              beforeEach(async () => {
                await init(true);
                await selectPlan(API_KEY_PLAN_ID);
                await selectApplication(APP_ID_ONE_API_KEY_SUBSCRIPTION);

                expectGetApi();
                expectGetSubscriptionsForApplication(
                  APP_ID_ONE_API_KEY_SUBSCRIPTION,
                  fakeSubscriptionResponse({
                    data: [fakeSubscription({ plan: 'plan-id', api: API_ID })],
                    metadata: {
                      'plan-id': { securityType: 'API_KEY' },
                    },
                  }),
                );
                fixture.detectChanges();
              });
              it('should show api key mode choice + only allow exclusive', async () => {
                const step3 = await harnessLoader.getHarness(SubscribeToApiCheckoutHarness);
                expect(await step3.isChooseApiKeyModeVisible()).toBeTruthy();

                const sharedApiKeyOption = await step3.getSharedApiKeyRadio();
                expect(await sharedApiKeyOption.isDisabled()).toEqual(true);

                const generatedApiKeyOption = await step3.getGeneratedApiKeyRadio();
                expect(await generatedApiKeyOption.isDisabled()).toEqual(false);
              });
              it('should create subscription', async () => {
                const step3 = await harnessLoader.getHarness(SubscribeToApiCheckoutHarness);
                const generatedApiKeyOption = await step3.getGeneratedApiKeyRadio();
                await generatedApiKeyOption.select();

                const subscribe = await getSubscribeButton();
                await subscribe?.click();

                expectPostCreateSubscription({
                  plan: API_KEY_PLAN_ID,
                  application: APP_ID_ONE_API_KEY_SUBSCRIPTION,
                  api_key_mode: 'EXCLUSIVE',
                });
              });
            });
            describe('When Shared API Key mode is disabled', () => {
              beforeEach(async () => {
                await init(false);
                await selectPlan(API_KEY_PLAN_ID);
                await selectApplication(APP_ID_ONE_API_KEY_SUBSCRIPTION);

                expectGetApi();
                expectGetSubscriptionsForApplication(
                  APP_ID_ONE_API_KEY_SUBSCRIPTION,
                  fakeSubscriptionResponse({
                    data: [fakeSubscription({ plan: 'plan-id', api: API_ID })],
                    metadata: {
                      'plan-id': { securityType: 'API_KEY' },
                    },
                  }),
                );
                fixture.detectChanges();
              });

              it('should not show api key mode selection', async () => {
                const step3 = await harnessLoader.getHarness(SubscribeToApiCheckoutHarness);
                expect(await step3.isChooseApiKeyModeVisible()).toBeFalsy();
              });
              it('should create subscription', async () => {
                const subscribe = await getSubscribeButton();
                await subscribe?.click();

                expectPostCreateSubscription({
                  plan: API_KEY_PLAN_ID,
                  application: APP_ID_ONE_API_KEY_SUBSCRIPTION,
                });
              });
            });
          });
          describe('When the existing API Key subscription is for a different API', () => {
            beforeEach(async () => {
              await init(true);
              await selectPlan(API_KEY_PLAN_ID);
              await selectApplication(APP_ID_ONE_API_KEY_SUBSCRIPTION);

              expectGetApi();
              expectGetSubscriptionsForApplication(
                APP_ID_ONE_API_KEY_SUBSCRIPTION,
                fakeSubscriptionResponse({
                  data: [fakeSubscription({ plan: 'plan-id', api: 'other-api' })],
                  metadata: {
                    'plan-id': { securityType: 'API_KEY' },
                  },
                }),
              );
              fixture.detectChanges();
            });
            it('should show api key mode choice', async () => {
              const step3 = await harnessLoader.getHarness(SubscribeToApiCheckoutHarness);
              expect(await step3.isChooseApiKeyModeVisible()).toBeTruthy();

              const sharedApiKeyOption = await step3.getSharedApiKeyRadio();
              expect(await sharedApiKeyOption.isDisabled()).toEqual(false);
              expect(await sharedApiKeyOption.isSelected()).toEqual(false);

              const generatedApiKeyOption = await step3.getGeneratedApiKeyRadio();
              expect(await generatedApiKeyOption.isDisabled()).toEqual(false);
              expect(await generatedApiKeyOption.isSelected()).toEqual(false);
            });
            it('should create subscription', async () => {
              const step3 = await harnessLoader.getHarness(SubscribeToApiCheckoutHarness);
              const sharedApiKeyOption = await step3.getSharedApiKeyRadio();
              await sharedApiKeyOption.select();

              const subscribe = await getSubscribeButton();
              await subscribe?.click();

              expectPostCreateSubscription({
                plan: API_KEY_PLAN_ID,
                application: APP_ID_ONE_API_KEY_SUBSCRIPTION,
                api_key_mode: 'SHARED',
              });
            });
          });
        });
        describe('When the API is Federated', () => {
          beforeEach(async () => {
            await init(true);
            await selectPlan(API_KEY_PLAN_ID);
            await selectApplication(APP_ID_ONE_API_KEY_SUBSCRIPTION);

            expectGetApi(fakeApi({ id: API_ID, definitionVersion: 'FEDERATED' }));
            fixture.detectChanges();
          });
          it('should not show api key mode choice', async () => {
            const step3 = await harnessLoader.getHarness(SubscribeToApiCheckoutHarness);
            expect(await step3.isChooseApiKeyModeVisible()).toBeFalsy();
          });
          it('should create subscription', async () => {
            const subscribe = await getSubscribeButton();
            await subscribe?.click();

            expectPostCreateSubscription({
              plan: API_KEY_PLAN_ID,
              application: APP_ID_ONE_API_KEY_SUBSCRIPTION,
            });
          });
        });
      });

      describe('When comment is NOT required + Terms and conditions NOT required', () => {
        beforeEach(async () => {
          await init(true);
          await selectPlan(API_KEY_PLAN_ID);
          await selectApplication();

          expectGetApi();
          fixture.detectChanges();
        });
        it('should allow subscribe without comment', async () => {
          const subscribeButton = await getSubscribeButton();
          expect(await subscribeButton?.isDisabled()).toEqual(false);
          await subscribeButton?.click();

          expectPostCreateSubscription({ plan: API_KEY_PLAN_ID, application: 'app-id' });
        });
        it('should subscribe with comment', async () => {
          const step3 = await harnessLoader.getHarness(SubscribeToApiCheckoutHarness);
          const messageBox = await step3.getMessageInput();
          await messageBox.setValue('My new message');

          const subscribeButton = await getSubscribeButton();
          expect(await subscribeButton?.isDisabled()).toEqual(false);
          await subscribeButton?.click();

          expectPostCreateSubscription({ plan: API_KEY_PLAN_ID, application: 'app-id', request: 'My new message' });
        });
      });
    });
  });

  describe('User subscribes to OAuth2 plan', () => {
    beforeEach(async () => {
      await init(true);
    });
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
    beforeEach(async () => {
      await init(true);
    });
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

  function expectGetSubscriptionsForApi(apiId: string, subscriptions: SubscriptionsResponse = fakeSubscriptionResponse({ data: [] })) {
    httpTestingController
      .expectOne(`${TESTING_BASE_URL}/subscriptions?apiId=${apiId}&statuses=PENDING&statuses=ACCEPTED&size=-1`)
      .flush(subscriptions);
  }

  function expectGetSubscriptionsForApplication(
    applicationId: string,
    subscriptions: SubscriptionsResponse = fakeSubscriptionResponse({ data: [] }),
  ) {
    httpTestingController
      .expectOne(
        `${TESTING_BASE_URL}/subscriptions?applicationId=${applicationId}&statuses=PENDING&statuses=ACCEPTED&statuses=PAUSED&size=-1`,
      )
      .flush(subscriptions);
  }

  function expectGetApplications(page: number = 1, applicationsResponse: ApplicationsResponse = fakeApplicationsResponse()) {
    httpTestingController
      .expectOne(`${TESTING_BASE_URL}/applications?page=${page}&size=9&forSubscriptions=true`)
      .flush(applicationsResponse);
  }

  function expectPostCreateSubscription(expectedCreateSubscription: CreateSubscription, response: Subscription = fakeSubscription()) {
    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/subscriptions`);

    expect(req.request.body).toEqual(expectedCreateSubscription);
    req.flush(response);
  }

  function expectGetPage(page: Page) {
    httpTestingController.expectOne(`${TESTING_BASE_URL}/apis/${API_ID}/pages/${page.id}?include=content`).flush(page);
  }

  async function canGoToNextStep(): Promise<boolean> {
    return await getNextStepButton()
      .then(btn => btn.isDisabled())
      .then(res => !res);
  }

  async function goToNextStep(): Promise<void> {
    return await getNextStepButton().then(btn => btn.click());
  }

  async function getNextStepButton(): Promise<MatButtonHarness> {
    return await harnessLoader.getHarness(MatButtonHarness.with({ text: 'Next' }));
  }

  function getSelectedApplicationName(): string | undefined {
    return document.getElementById('selected-application')?.innerHTML;
  }

  async function getSubscribeButton(): Promise<MatButtonHarness | null> {
    return await harnessLoader.getHarnessOrNull(MatButtonHarness.with({ text: 'Subscribe' }));
  }

  function getTitle(): string {
    return fixture.debugElement.query(By.css('.m3-title-large')).nativeElement.textContent;
  }

  async function selectPlan(planId: string): Promise<void> {
    const step1 = await harnessLoader.getHarness(SubscribeToApiChoosePlanHarness);
    await step1.selectPlanByPlanId(planId);
    await goToNextStep();
  }

  async function selectApplication(appId: string = APP_ID): Promise<void> {
    expectGetSubscriptionsForApi(API_ID);
    expectGetApplications(
      1,
      fakeApplicationsResponse({
        data: [
          fakeApplication({ id: APP_ID, name: APP_ID }),
          fakeApplication({ id: APP_ID_NO_SUBSCRIPTIONS, name: APP_ID_NO_SUBSCRIPTIONS, api_key_mode: 'UNSPECIFIED' }),
          fakeApplication({ id: APP_ID_ONE_API_KEY_SUBSCRIPTION, name: APP_ID_ONE_API_KEY_SUBSCRIPTION, api_key_mode: 'UNSPECIFIED' }),
        ],
      }),
    );
    fixture.detectChanges();
    const application = await harnessLoader.getHarness(RadioCardHarness.with({ title: appId }));
    await application.select();
    await goToNextStep();
  }
});
