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

import { ApiKeyMode, Application } from 'src/entities/application/Application';

import { Api, Plan } from '../../../../../entities/management-api-v2';

type SubscriptionCreationFormValues = {
  selectedApi: Api;
  selectedPlan: Plan;
  apiKeyMode?: ApiKeyMode;
  customApiKey?: string;
  selectedEntrypoint?: string;
  channel?: string;
  entrypointConfiguration?: any;
};

export const toNewSubscriptionEntity = (application: Application, values: SubscriptionCreationFormValues) => {
  return {
    api: values.selectedApi,
    subscriptionToCreate: {
      planId: values.selectedPlan.id,
      applicationId: application.id,
      ...(values.apiKeyMode != null
        ? {
            apiKeyMode: values.apiKeyMode,
          }
        : {}),
      ...(values.customApiKey != null
        ? {
            customApiKey: values.customApiKey,
          }
        : {}),
      ...(values.selectedPlan.definitionVersion === 'V4' && values.selectedPlan.mode === 'PUSH'
        ? {
            consumerConfiguration: {
              channel: values.channel ?? undefined,
              entrypointId: values.selectedEntrypoint ?? undefined,
              entrypointConfiguration: values.entrypointConfiguration ?? undefined,
            },
          }
        : undefined),
    },
  };
};
