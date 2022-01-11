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

import { ADMIN_USER, API_PUBLISHER_USER } from '../../fakers/users/users';
import { gio } from '../../commands/gravitee.commands';
import { fail } from 'assert';
import { BasicAuthentication } from '../../model/users';
import { Plan, PlanSecurityType } from '@model/plan';

describe('Create subscriptions', () => {
  it('Should create subscriptions', () => {
    subscribeAll(API_PUBLISHER_USER);
    subscribeAll(ADMIN_USER);
  });
});

function subscribeAll(user: BasicAuthentication) {
  gio
    .management(user)
    .applications()
    .getAll()
    .ok()
    .should((response) => {
      expect(response.body.length, 'No app found').greaterThan(0);
      const app = response.body[0];
      gio
        .management(user)
        .apis()
        .getAll()
        .ok()
        .should((response) => {
          expect(response.body.length, 'No api found').greaterThan(0);
          response.body.forEach((api) => {
            gio
              .management(user)
              .apisPlans()
              .getAll(api.id)
              .ok()
              .should((response) => {
                expect(response.body.length, 'No plan found').greaterThan(0);
                const plan = response.body.find((plan: Plan) => plan.securityDefinition === PlanSecurityType.API_KEY);
                if (plan) {
                  gio
                    .management(user)
                    .apisSubscriptions()
                    .create(api.id, app.id, plan.id)
                    .should((response) => {
                      if (response.status === 201) {
                        expect(response.status, 'Subscription created').eq(201);
                      } else if (response.status === 400) {
                        // @ts-ignore
                        expect(response.body.technicalCode, 'Ignore 400').eq('plan.subscribed');
                      } else {
                        fail('Error on subscriptions');
                      }
                    });
                } else {
                  fail('No API_KEY plan found');
                }
              });
          });
        });
    });
}
