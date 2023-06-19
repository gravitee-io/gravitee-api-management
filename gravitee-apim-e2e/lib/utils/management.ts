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
import { APIsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/APIsApi';
import { APIsApi as APIsV2Api } from '@gravitee/management-v2-webclient-sdk/src/lib';
import { forManagementAsAdminUser, forManagementV2AsApiUser } from './configuration';
import { APIPlansApi } from '@gravitee/management-webclient-sdk/src/lib/apis/APIPlansApi';
import { APIPlansApi as APIPlansV2Api } from '@gravitee/management-v2-webclient-sdk/src/lib';
import { LifecycleAction } from '@gravitee/management-webclient-sdk/src/lib/models/LifecycleAction';
import { ApplicationsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/ApplicationsApi';
import { ApiLifecycleState } from '@gravitee/management-webclient-sdk/src/lib/models/ApiLifecycleState';
import { ApiEntityStateEnum } from '@gravitee/management-webclient-sdk/src/lib/models/ApiEntity';
import { ApiType, ApiV4 } from '../management-v2-webclient-sdk/src/lib';

const apisResource = new APIsApi(forManagementAsAdminUser());
const apiPlansResource = new APIPlansApi(forManagementAsAdminUser());
const applicationsResource = new ApplicationsApi(forManagementAsAdminUser());
const apisV4Resource = new APIsV2Api(forManagementV2AsApiUser());
const apiPlansV4Resource = new APIPlansV2Api(forManagementV2AsApiUser());

/**
 * Teardown apis and applications
 * @param orgId
 * @param envId
 * @param apiIds
 * @param applicationIds
 */
export const teardownApisAndApplications = async (orgId: string, envId: string, apiIds?: string[], applicationIds?: string[]) => {
  for (const apiId of apiIds ?? []) {
    await deleteApi(orgId, envId, apiId);
  }

  for (const applicationId of applicationIds ?? []) {
    await deleteApplication(orgId, envId, applicationId);
  }
};

/**
 * Teardown V4 apis and applications
 * @param orgId
 * @param envId
 * @param apiIds
 * @param applicationIds
 */
export const teardownV4ApisAndApplications = async (orgId: string, envId: string, apiIds?: string[], applicationIds?: string[]) => {
  for (const apiId of apiIds ?? []) {
    await deleteV4Api(orgId, envId, apiId);
  }

  for (const applicationId of applicationIds ?? []) {
    await deleteApplication(orgId, envId, applicationId);
  }
};

/**
 * Steps : Stop API -> Close each api plan -> Un-publish the API -> Delete API
 * @param orgId
 * @param envId
 * @param apiId
 */
const deleteApi = async (orgId: string, envId: string, apiId: string) => {
  const apiToDelete = await apisResource.getApi({ api: apiId, envId, orgId });

  if (apiToDelete) {
    if (apiToDelete.state === ApiEntityStateEnum.STARTED) {
      // Stop API
      await apisResource.doApiLifecycleAction({
        envId,
        orgId,
        api: apiToDelete.id,
        action: LifecycleAction.STOP,
      });
    }
    // Close each api plan
    for (const planToClose of apiToDelete.plans) {
      await apiPlansResource.closeApiPlan({
        envId,
        orgId,
        plan: planToClose.id,
        api: apiToDelete.id,
      });
    }

    if (apiToDelete.lifecycle_state === ApiLifecycleState.PUBLISHED) {
      // Un-publish the API
      await apisResource.updateApi({
        envId,
        orgId,
        api: apiToDelete.id,
        updateApiEntity: {
          lifecycle_state: ApiLifecycleState.UNPUBLISHED,
          description: apiToDelete.description,
          name: apiToDelete.name,
          proxy: apiToDelete.proxy,
          version: apiToDelete.version,
          visibility: apiToDelete.visibility,
        },
      });
    }

    // Delete API
    await apisResource.deleteApi({
      envId,
      orgId,
      api: apiToDelete.id,
    });
  }
};

/**
 * Steps : Stop API -> Close each api plan -> Un-publish the API -> Delete API
 * @param orgId
 * @param envId
 * @param apiId
 */
const deleteV4Api = async (orgId: string, envId: string, apiId: string) => {
  const apiToDelete = (await apisV4Resource.getApi({ envId, apiId })) as ApiV4;

  if (apiToDelete) {
    if (apiToDelete.state === ApiEntityStateEnum.STARTED) {
      // Stop API
      await apisV4Resource.stopApi({
        envId,
        apiId: apiToDelete.id,
      });
    }

    if (apiToDelete.lifecycleState === ApiLifecycleState.PUBLISHED) {
      // Un-publish the API
      await apisV4Resource.updateApi({
        envId,
        apiId: apiToDelete.id,
        updateApi: {
          lifecycleState: ApiLifecycleState.UNPUBLISHED,
          description: apiToDelete.description,
          name: apiToDelete.name,
          apiVersion: apiToDelete.apiVersion,
          visibility: apiToDelete.visibility,
          definitionVersion: 'V4',
          endpointGroups: apiToDelete.endpointGroups,
          listeners: apiToDelete.listeners,
          analytics: apiToDelete.analytics,
          type: ApiType.MESSAGE,
        },
      });
    }

    // Delete API
    await apisV4Resource.deleteApi({
      envId,
      apiId: apiToDelete.id,
      closePlans: true,
    });
  }
};

/**
 * Delete application
 * @param orgId
 * @param envId
 * @param applicationId
 */
const deleteApplication = async (orgId: string, envId: string, applicationId: string) => {
  await applicationsResource.deleteApplication({
    envId,
    orgId,
    application: applicationId,
  });
};
