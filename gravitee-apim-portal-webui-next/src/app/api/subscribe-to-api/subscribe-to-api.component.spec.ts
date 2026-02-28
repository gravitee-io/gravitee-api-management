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
import { MatChipHarness } from '@angular/material/chips/testing';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';

import { GMD_FORM_STATE_STORE, GmdFormStateStore } from '@gravitee/gravitee-markdown';

import { TermsAndConditionsDialogHarness } from './components/terms-and-conditions-dialog/terms-and-conditions-dialog.harness';
import { SubscribeToApiCheckoutHarness } from './subscribe-to-api-checkout/subscribe-to-api-checkout.harness';
import { SubscribeToApiChooseApplicationHarness } from './subscribe-to-api-choose-application/subscribe-to-api-choose-application.harness';
import { SubscribeToApiChoosePlanHarness } from './subscribe-to-api-choose-plan/subscribe-to-api-choose-plan.harness';
import { SubscribeToApiComponent } from './subscribe-to-api.component';
import { ApiAccessHarness } from '../../../components/api-access/api-access.harness';
import { RadioCardHarness } from '../../../components/radio-card/radio-card.harness';
import { ConsumerConfigurationComponentHarness } from '../../../components/subscription/webhook/consumer-configuration/consumer-configuration.harness';
import { Api } from '../../../entities/api/api';
import { fakeApi } from '../../../entities/api/api.fixtures';
import { ApplicationsResponse } from '../../../entities/application/application';
import { fakeApplication, fakeApplicationsResponse } from '../../../entities/application/application.fixture';
import { Page } from '../../../entities/page/page';
import { fakePage } from '../../../entities/page/page.fixtures';
import { fakePlan } from '../../../entities/plan/plan.fixture';
import { SubscriptionForm } from '../../../entities/portal/subscription-form';
import {
  CreateSubscription,
  fakeSubscription,
  fakeSubscriptionResponse,
  Subscription,
  SubscriptionsResponse,
} from '../../../entities/subscription';
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
  const API = fakeApi({ id: API_ID, entrypoints: [ENTRYPOINT] });
  const KEYLESS_PLAN_ID = 'keyless-plan';
  const API_KEY_PLAN_ID = 'api-key-plan';
  const API_KEY_PLAN_ID_COMMENT_REQUIRED = 'api-key-plan-comment-required';
  const API_KEY_PLAN_ID_GENERAL_CONDITIONS = 'api-key-plan-general-conditions';
  const OAUTH2_PLAN_ID = 'oauth2-plan';
  const JWT_PLAN_ID = 'jwt-plan';
  const MTLS_PLAN_ID = 'mtls-plan';
  const PUSH_PLAN_ID = 'push-plan';
  const GENERAL_CONDITIONS_ID = 'page-id';
  const APP_ID = 'app-id';
  const APP_ID_NO_SUBSCRIPTIONS = 'app-id-no-subscriptions';
  const APP_ID_ONE_API_KEY_SUBSCRIPTION = 'app-id-one-api-key-subscription';
  const APP_ID_WITH_CLIENT_CERTIFICATE = 'app-id-with-client-certificate';
  const CONFIGURATION_KAFKA_SASL_MECHANISMS = '[PLAIN, SCRAM-SHA-256, SCRAM-SHA-512]';

  const init = async (sharedApiKeyModeEnabled: boolean, api: Api = API, subscriptionForm: SubscriptionForm | null = null) => {
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
            loadConfiguration: () => of({ portal: { kafkaSaslMechanisms: CONFIGURATION_KAFKA_SASL_MECHANISMS } }),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SubscribeToApiComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
    rootHarnessLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    component = fixture.componentInstance;
    component.api = api;
    fixture.detectChanges();

    expectGetSubscriptionForm(subscriptionForm);
    httpTestingController.expectOne(`${TESTING_BASE_URL}/apis/${API_ID}/plans?size=-1`).flush({
      data: [
        fakePlan({ id: KEYLESS_PLAN_ID, security: 'KEY_LESS' }),
        fakePlan({ id: API_KEY_PLAN_ID, security: 'API_KEY', comment_required: false, general_conditions: undefined }),
        fakePlan({
          id: API_KEY_PLAN_ID_COMMENT_REQUIRED,
          security: 'API_KEY',
          comment_required: true,
          general_conditions: undefined,
        }),
        fakePlan({
          id: API_KEY_PLAN_ID_GENERAL_CONDITIONS,
          security: 'API_KEY',
          general_conditions: GENERAL_CONDITIONS_ID,
        }),
        fakePlan({ id: OAUTH2_PLAN_ID, security: 'OAUTH2', general_conditions: undefined }),
        fakePlan({ id: JWT_PLAN_ID, security: 'JWT', general_conditions: undefined }),
        fakePlan({ id: MTLS_PLAN_ID, security: 'MTLS', general_conditions: undefined }),
        fakePlan({ id: PUSH_PLAN_ID, comment_required: true, general_conditions: undefined, mode: 'PUSH' }),
      ],
    });
    fixture.detectChanges();
  };

  afterEach(() => {
    httpTestingController.verify();
  });

  describe('User subscribes to Push plan', () => {
    describe('selects push plan', () => {
      it('should be able to go to consumer configuration form', async () => {
        await init(true);
        const step1 = await harnessLoader.getHarness(SubscribeToApiChoosePlanHarness);
        await step1.selectPlanByPlanId(PUSH_PLAN_ID);
        await goToNextStep();

        expectGetSubscriptionsForApi(
          API_ID,
          fakeSubscriptionResponse({
            data: [fakeSubscription({ status: 'CLOSED', plan: PUSH_PLAN_ID, application: '3' })],
            metadata: {
              [PUSH_PLAN_ID]: {
                planMode: 'PUSH',
              },
            },
          }),
        );
        const APP_ID_1 = 'app-id-1';
        expectGetApplications(
          1,
          fakeApplicationsResponse({
            data: [fakeApplication({ id: APP_ID_1, name: 'App 1' })],
          }),
        );
        fixture.detectChanges();

        expect(getTitle()).toEqual('Choose an application');
        const app1 = await harnessLoader.getHarnessOrNull(RadioCardHarness.with({ title: 'App 1' }));
        await app1?.select();
        await goToNextStep();
        fixture.detectChanges();

        expect(getTitle()).toEqual('Configure Consumer');
        const consumerConfigurationStep = await harnessLoader.getHarness(ConsumerConfigurationComponentHarness);
        expect(await canGoToNextStep()).toEqual(false);
        await consumerConfigurationStep.setInputTextValueFromControlName('callbackUrl', 'https://pawels.example.com');
        expect(await canGoToNextStep()).toEqual(true);
        await goToNextStep();
        fixture.detectChanges();

        expect(getTitle()).toEqual('Checkout');
        const checkout = await harnessLoader.getHarness(SubscribeToApiCheckoutHarness);
        const subscribeButton = await getSubscribeButton();
        expect(await checkout.isSubscriptionFormVisible()).toEqual(false);
        expect(await subscribeButton?.isDisabled()).toEqual(false);
        await subscribeButton?.click();

        expectPostCreateSubscription({
          plan: PUSH_PLAN_ID,
          application: APP_ID_1,
          configuration: {
            channel: '',
            entrypointId: 'webhook',
            entrypointConfiguration: {
              callbackUrl: 'https://pawels.example.com',
              headers: [],
              retry: {
                retryOption: 'No Retry',
              },
              ssl: { hostnameVerifier: false, trustAll: false },
              auth: { type: 'none' },
            },
          },
        });
      });
    });
  });

  describe('User subscribes to Keyless plan', () => {
    describe('Step 1 -- Choose a plan', () => {
      it('should be able to go to step 3 once plan chosen', async () => {
        await init(true);

        const step1 = await harnessLoader.getHarness(SubscribeToApiChoosePlanHarness);
        expect(step1).toBeTruthy();

        expect(await step1.isPlanSelected(KEYLESS_PLAN_ID)).toEqual(false);
        expect(await canGoToNextStep()).toEqual(false);

        await step1.selectPlanByPlanId(KEYLESS_PLAN_ID);
        expect(await step1.isPlanSelected(KEYLESS_PLAN_ID)).toEqual(true);

        await goToNextStep();

        fixture.detectChanges();
        expect(getTitle()).toEqual('Checkout');
      });
    });
    describe('Step 3 -- Checkout', () => {
      describe('V4 Proxy', () => {
        beforeEach(async () => {
          await init(true, fakeApi({ id: API_ID, type: 'PROXY', definitionVersion: 'V4', entrypoints: [ENTRYPOINT] }));

          const step1 = await harnessLoader.getHarness(SubscribeToApiChoosePlanHarness);
          await step1.selectPlanByPlanId(KEYLESS_PLAN_ID);
          await goToNextStep();
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

      describe('V4 Native', () => {
        const KAFKA_ENTRYPOINT = 'my.kafka.entrypoint:9092';
        beforeEach(async () => {
          await init(
            true,
            fakeApi({
              id: API_ID,
              type: 'NATIVE',
              definitionVersion: 'V4',
              entrypoints: [KAFKA_ENTRYPOINT],
            }),
          );

          const step1 = await harnessLoader.getHarness(SubscribeToApiChoosePlanHarness);
          await step1.selectPlanByPlanId(KEYLESS_PLAN_ID);
          await goToNextStep();
          fixture.detectChanges();
        });
        it('should see checkout information', async () => {
          expect(fixture.debugElement.query(By.css('app-subscription-info'))).toBeTruthy();
          const apiAccess = await harnessLoader.getHarness(ApiAccessHarness);
          expect(apiAccess).toBeTruthy();

          expect(await apiAccess.getSslConfig()).toBeTruthy();
          expect(await apiAccess.getProducerCommand()).toContain(KAFKA_ENTRYPOINT);
        });
        it('should not show subscribe button', async () => {
          expect(await getSubscribeButton()).toEqual(null);
        });
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

          expect(await getSelectedApplicationName()).toEqual('App 10');
          await goToNextStep();

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
          expect(pageContent).toBeTruthy();

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
      describe('When a subscription form is enabled', () => {
        const subscriptionForm: SubscriptionForm = {
          gmdContent: '# Subscription form',
        };
        beforeEach(async () => {
          await init(true, API, subscriptionForm);
          await selectPlan(API_KEY_PLAN_ID);
          await selectApplication();

          fixture.detectChanges();
        });
        it('should render the subscription form', async () => {
          const step3 = await harnessLoader.getHarness(SubscribeToApiCheckoutHarness);
          expect(await step3.isSubscriptionFormVisible()).toEqual(true);
        });
        it('should block subscribe when required form field is invalid', async () => {
          const store = fixture.debugElement.injector.get<GmdFormStateStore>(GMD_FORM_STATE_STORE);
          store.updateField({
            id: 'field-1',
            fieldKey: 'company',
            value: '',
            valid: false,
            required: true,
            touched: true,
            validationErrors: ['required'],
          });
          fixture.detectChanges();

          const subscribeButton = await getSubscribeButton();
          expect(await subscribeButton?.isDisabled()).toEqual(true);
        });
      });
      describe('API Key Management', () => {
        describe('When a chosen application has no existing subscriptions', () => {
          beforeEach(async () => {
            await init(true);
            await selectPlan(API_KEY_PLAN_ID);
            await selectApplication(APP_ID_NO_SUBSCRIPTIONS);

            expectGetSubscriptionsForApplication(
              APP_ID_NO_SUBSCRIPTIONS,
              fakeSubscriptionResponse({
                data: [],
                metadata: {},
              }),
            );
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
            await init(true, fakeApi({ id: API_ID, definitionVersion: 'FEDERATED' }));
            await selectPlan(API_KEY_PLAN_ID);
            await selectApplication(APP_ID_ONE_API_KEY_SUBSCRIPTION);

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

          fixture.detectChanges();
        });
        it('should allow subscribe without comment', async () => {
          const subscribeButton = await getSubscribeButton();
          expect(await subscribeButton?.isDisabled()).toEqual(false);
          await subscribeButton?.click();

          expectPostCreateSubscription({ plan: API_KEY_PLAN_ID, application: 'app-id' });
        });
      });
    });
  });

  describe('User subscribes to OAuth2 plan', () => {
    beforeEach(async () => {
      await init(true);
    });
    describe('Step 1 -- Choose a plan', () => {
      it('should be enabled', async () => {
        const step1 = await harnessLoader.getHarness(SubscribeToApiChoosePlanHarness);
        expect(step1).toBeTruthy();

        expect(await step1.isPlanSelected(OAUTH2_PLAN_ID)).toEqual(false);
        expect(await step1.isPlanDisabled(OAUTH2_PLAN_ID)).toEqual(false);
        await step1.selectPlanByPlanId(OAUTH2_PLAN_ID);

        expect(await canGoToNextStep()).toEqual(true);
      });
    });

    describe('Step 2 -- Choose an application', () => {
      const APP_ID = 'app-id';
      beforeEach(async () => {
        await selectPlan(OAUTH2_PLAN_ID);
      });

      it('should disable application with existing subscription to plan', async () => {
        expectGetSubscriptionsForApi(
          API_ID,
          fakeSubscriptionResponse({
            data: [fakeSubscription({ status: 'ACCEPTED', plan: OAUTH2_PLAN_ID, application: APP_ID })],
            metadata: {
              [OAUTH2_PLAN_ID]: {
                securityType: 'OAUTH2',
              },
            },
          }),
        );
        expectGetApplications(
          1,
          fakeApplicationsResponse({
            data: [fakeApplication({ id: APP_ID, name: 'App 1' })],
          }),
        );
        fixture.detectChanges();

        const app1 = await harnessLoader.getHarness(RadioCardHarness.with({ title: 'App 1' }));
        expect(await app1.isDisabled()).toEqual(true);
      });
      it('should disable application with existing OAuth2 subscription to another plan', async () => {
        const anotherOAuth2Plan = 'another-plan';
        expectGetSubscriptionsForApi(
          API_ID,
          fakeSubscriptionResponse({
            data: [fakeSubscription({ status: 'PENDING', plan: anotherOAuth2Plan, application: APP_ID })],
            metadata: {
              [anotherOAuth2Plan]: {
                securityType: 'OAUTH2',
              },
            },
          }),
        );
        expectGetApplications(
          1,
          fakeApplicationsResponse({
            data: [fakeApplication({ id: APP_ID, name: 'App 1' })],
          }),
        );
        fixture.detectChanges();

        const app1 = await harnessLoader.getHarness(RadioCardHarness.with({ title: 'App 1' }));
        expect(await app1.isDisabled()).toEqual(true);
      });
      it('should disable application with existing JWT subscription to another plan', async () => {
        const anotherJWTPlan = 'another-plan';
        expectGetSubscriptionsForApi(
          API_ID,
          fakeSubscriptionResponse({
            data: [fakeSubscription({ status: 'PENDING', plan: anotherJWTPlan, application: APP_ID })],
            metadata: {
              [anotherJWTPlan]: {
                securityType: 'JWT',
              },
            },
          }),
        );
        expectGetApplications(
          1,
          fakeApplicationsResponse({
            data: [fakeApplication({ id: APP_ID, name: 'App 1' })],
          }),
        );
        fixture.detectChanges();

        const app1 = await harnessLoader.getHarness(RadioCardHarness.with({ title: 'App 1' }));
        expect(await app1.isDisabled()).toEqual(true);
      });
      it('should disable application missing a Client ID', async () => {
        expectGetSubscriptionsForApi(
          API_ID,
          fakeSubscriptionResponse({
            data: [],
          }),
        );
        expectGetApplications(
          1,
          fakeApplicationsResponse({
            data: [fakeApplication({ id: APP_ID, name: 'App 1', hasClientId: false })],
          }),
        );
        fixture.detectChanges();

        const app1 = await harnessLoader.getHarness(RadioCardHarness.with({ title: 'App 1' }));
        expect(await app1.isDisabled()).toEqual(true);
      });
      it('should select valid Application', async () => {
        expectGetSubscriptionsForApi(
          API_ID,
          fakeSubscriptionResponse({
            data: [fakeSubscription({ id: 'apikey-sub', plan: API_KEY_PLAN_ID, status: 'ACCEPTED' })],
            metadata: {
              [API_KEY_PLAN_ID]: {
                planMode: 'STANDARD',
                securityType: 'API_KEY',
              },
            },
          }),
        );
        expectGetApplications(
          1,
          fakeApplicationsResponse({
            data: [fakeApplication({ id: APP_ID, name: 'App 1', hasClientId: true })],
          }),
        );
        fixture.detectChanges();

        const app1 = await harnessLoader.getHarness(RadioCardHarness.with({ title: 'App 1' }));
        expect(await app1.isDisabled()).toEqual(false);
      });
    });

    describe('Step 3 -- Checkout', () => {
      beforeEach(async () => {
        await selectPlan(OAUTH2_PLAN_ID);
        await selectApplication();

        fixture.detectChanges();
      });
      it('should subscribe', async () => {
        const subscribeButton = await getSubscribeButton();
        expect(await subscribeButton?.isDisabled()).toEqual(false);
        await subscribeButton?.click();

        expectPostCreateSubscription({ plan: OAUTH2_PLAN_ID, application: 'app-id' });
      });
    });
  });

  describe('User subscribes to JWT plan', () => {
    beforeEach(async () => {
      await init(true);
    });
    describe('Step 1 -- Choose a plan', () => {
      it('should be enabled', async () => {
        const step1 = await harnessLoader.getHarness(SubscribeToApiChoosePlanHarness);
        expect(step1).toBeTruthy();

        expect(await step1.isPlanSelected(JWT_PLAN_ID)).toEqual(false);
        expect(await step1.isPlanDisabled(JWT_PLAN_ID)).toEqual(false);
        await step1.selectPlanByPlanId(JWT_PLAN_ID);

        expect(await canGoToNextStep()).toEqual(true);
      });
    });

    describe('Step 2 -- Choose an application', () => {
      const APP_ID = 'app-id';
      beforeEach(async () => {
        await selectPlan(JWT_PLAN_ID);
      });

      it('should disable application with existing subscription to plan', async () => {
        expectGetSubscriptionsForApi(
          API_ID,
          fakeSubscriptionResponse({
            data: [fakeSubscription({ status: 'ACCEPTED', plan: JWT_PLAN_ID, application: APP_ID })],
            metadata: {
              [JWT_PLAN_ID]: {
                securityType: 'JWT',
              },
            },
          }),
        );
        expectGetApplications(
          1,
          fakeApplicationsResponse({
            data: [fakeApplication({ id: APP_ID, name: 'App 1' })],
          }),
        );
        fixture.detectChanges();

        const app1 = await harnessLoader.getHarness(RadioCardHarness.with({ title: 'App 1' }));
        expect(await app1.isDisabled()).toEqual(true);
      });
      it('should disable application with existing OAuth2 subscription to another plan', async () => {
        const anotherOAuth2Plan = 'another-plan';
        expectGetSubscriptionsForApi(
          API_ID,
          fakeSubscriptionResponse({
            data: [fakeSubscription({ status: 'PENDING', plan: anotherOAuth2Plan, application: APP_ID })],
            metadata: {
              [anotherOAuth2Plan]: {
                securityType: 'OAUTH2',
              },
            },
          }),
        );
        expectGetApplications(
          1,
          fakeApplicationsResponse({
            data: [fakeApplication({ id: APP_ID, name: 'App 1' })],
          }),
        );
        fixture.detectChanges();

        const app1 = await harnessLoader.getHarness(RadioCardHarness.with({ title: 'App 1' }));
        expect(await app1.isDisabled()).toEqual(true);
      });
      it('should disable application with existing JWT subscription to another plan', async () => {
        const anotherJWTPlan = 'another-plan';
        expectGetSubscriptionsForApi(
          API_ID,
          fakeSubscriptionResponse({
            data: [fakeSubscription({ status: 'PENDING', plan: anotherJWTPlan, application: APP_ID })],
            metadata: {
              [anotherJWTPlan]: {
                securityType: 'JWT',
              },
            },
          }),
        );
        expectGetApplications(
          1,
          fakeApplicationsResponse({
            data: [fakeApplication({ id: APP_ID, name: 'App 1' })],
          }),
        );
        fixture.detectChanges();

        const app1 = await harnessLoader.getHarness(RadioCardHarness.with({ title: 'App 1' }));
        expect(await app1.isDisabled()).toEqual(true);
      });
      it('should disable application missing a Client ID', async () => {
        expectGetSubscriptionsForApi(
          API_ID,
          fakeSubscriptionResponse({
            data: [],
          }),
        );
        expectGetApplications(
          1,
          fakeApplicationsResponse({
            data: [fakeApplication({ id: APP_ID, name: 'App 1', hasClientId: false })],
          }),
        );
        fixture.detectChanges();

        const app1 = await harnessLoader.getHarness(RadioCardHarness.with({ title: 'App 1' }));
        expect(await app1.isDisabled()).toEqual(true);
      });
      it('should select valid Application', async () => {
        expectGetSubscriptionsForApi(
          API_ID,
          fakeSubscriptionResponse({
            data: [fakeSubscription({ id: 'apikey-sub', plan: API_KEY_PLAN_ID, status: 'ACCEPTED' })],
            metadata: {
              [API_KEY_PLAN_ID]: {
                planMode: 'STANDARD',
                securityType: 'API_KEY',
              },
            },
          }),
        );
        expectGetApplications(
          1,
          fakeApplicationsResponse({
            data: [fakeApplication({ id: APP_ID, name: 'App 1', hasClientId: true })],
          }),
        );
        fixture.detectChanges();

        const app1 = await harnessLoader.getHarness(RadioCardHarness.with({ title: 'App 1' }));
        expect(await app1.isDisabled()).toEqual(false);
      });
    });

    describe('Step 3 -- Checkout', () => {
      beforeEach(async () => {
        await selectPlan(JWT_PLAN_ID);
        await selectApplication();

        fixture.detectChanges();
      });
      it('should subscribe', async () => {
        const subscribeButton = await getSubscribeButton();
        expect(await subscribeButton?.isDisabled()).toEqual(false);
        await subscribeButton?.click();

        expectPostCreateSubscription({ plan: JWT_PLAN_ID, application: 'app-id' });
      });
    });
  });

  describe('User subscribes to mTLS plan', () => {
    beforeEach(async () => {
      await init(true);
    });
    describe('Step 1 -- Choose a plan', () => {
      it('should be enabled', async () => {
        const step1 = await harnessLoader.getHarness(SubscribeToApiChoosePlanHarness);
        expect(step1).toBeTruthy();

        expect(await step1.isPlanSelected(MTLS_PLAN_ID)).toEqual(false);
        expect(await step1.isPlanDisabled(MTLS_PLAN_ID)).toEqual(false);
        await step1.selectPlanByPlanId(MTLS_PLAN_ID);

        expect(await canGoToNextStep()).toEqual(true);
      });
    });

    describe('Step 2 -- Choose an application', () => {
      const APP_ID = 'app-id';
      beforeEach(async () => {
        await selectPlan(MTLS_PLAN_ID);
      });

      it('should disable application with existing subscription to plan', async () => {
        expectGetSubscriptionsForApi(
          API_ID,
          fakeSubscriptionResponse({
            data: [fakeSubscription({ status: 'ACCEPTED', plan: MTLS_PLAN_ID, application: APP_ID })],
            metadata: {
              [MTLS_PLAN_ID]: {
                securityType: 'MTLS',
              },
            },
          }),
        );
        expectGetApplications(
          1,
          fakeApplicationsResponse({
            data: [fakeApplication({ id: APP_ID, name: 'App 1' })],
          }),
        );
        fixture.detectChanges();

        const app1 = await harnessLoader.getHarness(RadioCardHarness.with({ title: 'App 1' }));
        expect(await app1.isDisabled()).toEqual(true);
      });
      it('should disable application without TLS Client Certificate', async () => {
        expectGetSubscriptionsForApi(
          API_ID,
          fakeSubscriptionResponse({
            data: [],
            metadata: {},
          }),
        );
        expectGetApplications(
          1,
          fakeApplicationsResponse({
            data: [fakeApplication({ id: APP_ID, name: 'App 1', settings: {} })],
          }),
        );
        fixture.detectChanges();

        const app1 = await harnessLoader.getHarness(RadioCardHarness.with({ title: 'App 1' }));
        expect(await app1.isDisabled()).toEqual(true);
      });
      it('should disable application with valid mTLS plan for API', async () => {
        const anotherMtlsPlan = 'another-plan';
        expectGetSubscriptionsForApi(
          API_ID,
          fakeSubscriptionResponse({
            data: [fakeSubscription({ status: 'PENDING', plan: anotherMtlsPlan, application: APP_ID })],
            metadata: {
              [anotherMtlsPlan]: {
                securityType: 'MTLS',
              },
            },
          }),
        );
        expectGetApplications(
          1,
          fakeApplicationsResponse({
            data: [fakeApplication({ id: APP_ID, name: 'App 1' })],
          }),
        );
        fixture.detectChanges();

        const app1 = await harnessLoader.getHarness(RadioCardHarness.with({ title: 'App 1' }));
        expect(await app1.isDisabled()).toEqual(true);
      });
    });

    describe('Step 3 -- Checkout', () => {
      beforeEach(async () => {
        await selectPlan(MTLS_PLAN_ID);
        await selectApplication(APP_ID_WITH_CLIENT_CERTIFICATE);

        fixture.detectChanges();
      });
      it('should subscribe', async () => {
        const subscribeButton = await getSubscribeButton();
        expect(await subscribeButton?.isDisabled()).toEqual(false);
        await subscribeButton?.click();

        expectPostCreateSubscription({ plan: MTLS_PLAN_ID, application: APP_ID_WITH_CLIENT_CERTIFICATE });
      });
    });
  });

  function expectGetSubscriptionsForApi(apiId: string, subscriptions: SubscriptionsResponse = fakeSubscriptionResponse({ data: [] })) {
    httpTestingController
      .expectOne(req => {
        return (
          req.url === `${TESTING_BASE_URL}/subscriptions` &&
          req.params.get('apiIds') === apiId &&
          req.params.getAll('statuses')?.includes('PENDING') === true &&
          req.params.getAll('statuses')?.includes('ACCEPTED') === true &&
          req.params.get('size') === '-1'
        );
      })
      .flush(subscriptions);
  }

  function expectGetSubscriptionsForApplication(
    applicationId: string,
    subscriptions: SubscriptionsResponse = fakeSubscriptionResponse({ data: [] }),
  ) {
    httpTestingController
      .expectOne(req => {
        return (
          req.url === `${TESTING_BASE_URL}/subscriptions` &&
          req.params.get('applicationIds') === applicationId &&
          req.params.getAll('statuses')?.includes('PENDING') === true &&
          req.params.getAll('statuses')?.includes('ACCEPTED') === true &&
          req.params.getAll('statuses')?.includes('PAUSED') === true &&
          req.params.get('size') === '-1'
        );
      })
      .flush(subscriptions);
  }

  function expectGetApplications(page: number = 1, applicationsResponse: ApplicationsResponse = fakeApplicationsResponse()) {
    httpTestingController
      .expectOne(`${TESTING_BASE_URL}/applications?page=${page}&size=9&forSubscription=true`)
      .flush(applicationsResponse);
  }

  function expectGetSubscriptionForm(subscriptionForm: SubscriptionForm | null) {
    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/subscription-form`);
    if (subscriptionForm) {
      req.flush(subscriptionForm);
    } else {
      req.flush(null, { status: 404, statusText: 'Not Found' });
    }
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

  async function getSelectedApplicationName(): Promise<string | null> {
    return await harnessLoader
      .getHarness(MatChipHarness)
      .then(chip => chip.getText())
      .catch(_ => null);
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
          fakeApplication({
            id: APP_ID_ONE_API_KEY_SUBSCRIPTION,
            name: APP_ID_ONE_API_KEY_SUBSCRIPTION,
            api_key_mode: 'UNSPECIFIED',
          }),
          fakeApplication({
            id: APP_ID_WITH_CLIENT_CERTIFICATE,
            name: APP_ID_WITH_CLIENT_CERTIFICATE,
            settings: { tls: { client_certificate: 'certificate' } },
          }),
        ],
      }),
    );
    fixture.detectChanges();
    const application = await harnessLoader.getHarness(RadioCardHarness.with({ title: appId }));
    await application.select();
    await goToNextStep();
  }
});
