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
import { ApiApi, GetPageByApiIdAndPageIdIncludeEnum } from '@portal-apis/ApiApi';
import { ANONYMOUS, forPortal, SIMPLE_USER } from '@client-conf/*';
import { authorized, unauthorized } from '@lib/jest-utils';
import { describe, test } from '@jest/globals';
import { ApplicationApi } from '@portal-apis/ApplicationApi';
import { AuthenticationApi } from '@portal-apis/AuthenticationApi';
import { PortalApi } from '@portal-apis/PortalApi';
import { GroupApi } from '@portal-apis/GroupApi';
import { SubscriptionApi } from '@portal-apis/SubscriptionApi';
import { UsersApi } from '@portal-apis/UsersApi';
import { UserApi } from '@portal-apis/UserApi';
const apiId = 'API';
const pageId = 'PAGE';
const applicationId = 'APP';
const memberId = 'MEMBER';
const logId = 'LOG';
const identity = 'identity';
const token = 'token';
const groupId = 'GROUP';
const subscriptionId = 'subscriptionId';
const apiKey = 'apiKey';
const userId = 'user';
const notificationId = 'notificationId';
const categoryId = 'categoryId';

function portalAuthorizationCases({ auth = ANONYMOUS }) {
  const configuration = forPortal({ auth });
  const apiPortalApi = new ApiApi(configuration);
  const applicationsApi = new ApplicationApi(configuration);
  const authenticationPortalApi = new AuthenticationApi(configuration);
  const portalApi = new PortalApi(configuration);
  const groupApi = new GroupApi(configuration);
  const subscriptionApi = new SubscriptionApi(configuration);
  const usersApi = new UsersApi(configuration);
  const userApi = new UserApi(configuration);

  const authorizedCasesForAnonymous = [
    { title: 'GET /apis', method: 'getApisRaw', params: {} },
    { title: 'POST /apis/_search', method: 'searchApisRaw', params: { q: '' } },
    { title: 'GET /apis/links', method: 'getApiLinksRaw', params: { apiId } },
    { title: 'GET /apis/metrics', method: 'getApiMetricsByApiIdRaw', params: { apiId } },
    { title: 'GET /apis/:apiId/pages', method: 'getPagesByApiIdRaw', params: { apiId } },
    { title: 'GET /apis/:apiId/pages/:pageId', method: 'getPageByApiIdAndPageIdRaw', params: { apiId, pageId } },
    {
      title: 'GET /apis/:apiId/pages/:pageId/content',
      method: 'getPageByApiIdAndPageIdRaw',
      params: { apiId, pageId, include: [GetPageByApiIdAndPageIdIncludeEnum.Content] },
    },
    { title: 'GET /apis/:apiId/picture', method: 'getPictureByApiIdRaw', params: { apiId } },
    { title: 'GET /apis/:apiId/plans', method: 'getApiPlansByApiIdRaw', params: { apiId } },
    { title: 'GET /apis/:apiId/ratings', method: 'getApiRatingsByApiIdRaw', params: { apiId } },
    {
      title: 'POST /auth/oauth2/:identity',
      method: 'exchangeAuthorizationCodeRaw',
      params: { identity },
      api: authenticationPortalApi,
    },
    {
      title: 'POST /auth/oauth2/:identity/_exchange',
      method: 'tokenExchangeRaw',
      params: { identity, token },
      api: authenticationPortalApi,
    },
    { title: 'GET /configuration', method: 'getPortalConfigurationRaw', api: portalApi },
    { title: 'GET /configuration/links', method: 'getPortalLinksRaw', api: portalApi },
    { title: 'GET /configuration/applications/roles', method: 'getApplicationRolesRaw', api: portalApi },
    { title: 'GET /configuration/applications/types', method: 'getEnabledApplicationTypesRaw', api: portalApi },
    { title: 'GET /configuration/applications/identities', method: 'getPortalIdentityProvidersRaw', api: portalApi },
    {
      title: 'GET /configuration/applications/identities/:identityProviderId',
      method: 'getPortalIdentityProviderRaw',
      api: portalApi,
      params: { identityProviderId: '' },
    },
    { title: 'GET /info', method: 'getPortalInformationRaw', api: portalApi },
    { title: 'GET /pages', method: 'getPagesRaw', api: portalApi, params: {} },
    {
      title: 'GET /pages/:pageId/content',
      method: 'getPageContentByApiIdAndPageId',
      api: apiPortalApi,
      params: { apiId, pageId },
    },
    { title: 'POST /users/registration', method: 'registerNewUserRaw', api: usersApi, params: {} },
    { title: 'POST /users/registration/_finalize', method: 'finalizeUserRegistrationRaw', api: usersApi, params: {} },
    { title: 'POST /users/_rest_password', method: 'resetUserPasswordRaw', api: usersApi, params: {} },
    { title: 'GET /categories', method: 'getCategoriesRaw', api: portalApi, params: {} },
    { title: 'GET /categories/:categoryId', method: 'getCategoryByCategoryIdRaw', api: portalApi, params: { categoryId } },
    {
      title: 'GET /categories/:categoryId/picture',
      method: 'getPictureByCategoryIdRaw',
      api: portalApi,
      params: { categoryId },
    },
  ];

  const unauthorizedCasesForAnonymous = [
    { title: 'POST /apis/:apiId/rating', method: 'createApiRatingRaw', params: { apiId } },
    { title: 'GET /apis/:apiId/subscribers', method: 'getSubscriberApplicationsByApiIdRaw', params: { apiId } },
    { title: 'GET /applications', method: 'getApplicationsRaw', params: {}, api: applicationsApi },
    { title: 'POST /applications', method: 'createApplicationRaw', params: {}, api: applicationsApi },
    {
      title: 'GET /applications/:applicationId',
      method: 'getApplicationByApplicationIdRaw',
      params: { applicationId },
      api: applicationsApi,
    },
    {
      title: 'PUT /applications/:applicationId',
      method: 'updateApplicationByApplicationIdRaw',
      params: { applicationId },
      api: applicationsApi,
    },
    {
      title: 'DELETE /applications/:applicationId',
      method: 'deleteApplicationByApplicationIdRaw',
      params: { applicationId },
      api: applicationsApi,
    },
    {
      title: 'GET /applications/:applicationId/picture',
      method: 'getApplicationPictureByApplicationIdRaw',
      params: { applicationId },
      api: applicationsApi,
    },
    {
      title: 'GET /applications/:applicationId/notification',
      method: 'getNotificationsByApplicationIdRaw',
      params: { applicationId },
      api: applicationsApi,
    },
    {
      title: 'PUT /applications/:applicationId/notification',
      method: 'updateApplicationNotificationsRaw',
      params: { applicationId },
      api: applicationsApi,
    },
    {
      title: 'GET /applications/:applicationId/members',
      method: 'getMembersByApplicationIdRaw',
      params: { applicationId },
      api: applicationsApi,
    },
    {
      title: 'POST /applications/:applicationId/members',
      method: 'createApplicationMemberRaw',
      params: { applicationId },
      api: applicationsApi,
    },
    {
      title: 'POST /applications/:applicationId/members/_transfer_ownership',
      method: 'transferMemberOwnershipRaw',
      params: { applicationId },
      api: applicationsApi,
    },
    {
      title: 'GET /applications/:applicationId/members/:memberId',
      method: 'getApplicationMemberByApplicationIdAndMemberIdRaw',
      params: { applicationId, memberId },
      api: applicationsApi,
    },
    {
      title: 'PUT /applications/:applicationId/members/:memberId',
      method: 'updateApplicationMemberByApplicationIdAndMemberIdRaw',
      params: { applicationId, memberId },
      api: applicationsApi,
    },
    {
      title: 'DELETE /applications/:applicationId/members/:memberId',
      method: 'deleteApplicationMemberRaw',
      params: { applicationId, memberId },
      api: applicationsApi,
    },
    {
      title: 'GET /applications/:applicationId/analytics',
      method: 'getApplicationAnalyticsRaw',
      params: { applicationId },
      api: applicationsApi,
    },
    {
      title: 'GET /applications/:applicationId/logs',
      method: 'getApplicationLogsRaw',
      params: { applicationId },
      api: applicationsApi,
    },
    {
      title: 'POST /applications/:applicationId/_export',
      method: 'exportApplicationLogsByApplicationIdRaw',
      params: { applicationId },
      api: applicationsApi,
    },
    {
      title: 'GET /applications/:applicationId/logs/:logId',
      method: 'getApplicationLogsRaw',
      params: { applicationId, logId },
      api: applicationsApi,
    },
    {
      title: 'POST /applications/:applicationId/_renew_secret',
      method: 'renewApplicationSecretRaw',
      params: { applicationId },
      api: applicationsApi,
    },
    { title: 'POST /auth/login', method: 'loginRaw', params: { authorization: '' }, api: authenticationPortalApi },
    { title: 'POST /auth/logout', method: 'logoutRaw', api: authenticationPortalApi },
    { title: 'GET /groups', method: 'getGroupsRaw', api: groupApi, params: {} },
    { title: 'GET /groups/:groupId/members', method: 'getMembersByGroupIdRaw', api: groupApi, params: { groupId } },
    { title: 'GET /subscriptions', method: 'getSubscriptionsRaw', api: subscriptionApi, params: {} },
    { title: 'POST /subscriptions', method: 'createSubscriptionRaw', api: subscriptionApi, params: {} },
    {
      title: 'GET /subscriptions/:subscriptionId',
      method: 'getSubscriptionByIdRaw',
      api: subscriptionApi,
      params: { subscriptionId },
    },
    {
      title: 'POST /subscriptions/:subscriptionId/_close',
      method: 'closeSubscriptionRaw',
      api: subscriptionApi,
      params: { subscriptionId },
    },
    {
      title: 'POST /subscriptions/:subscriptionId/keys/_renew',
      method: 'renewKeySubscriptionRaw',
      api: subscriptionApi,
      params: { subscriptionId },
    },
    {
      title: 'POST /subscriptions/:subscriptionId/keys/:apiKey/_revoke',
      method: 'revokeKeySubscriptionRaw',
      api: subscriptionApi,
      params: { subscriptionId, apiKey },
    },
    { title: 'GET /tickets', method: 'getTicketsRaw', api: portalApi, params: {} },
    { title: 'POST /users/_search', method: 'getUsersRaw', api: usersApi, params: {} },
    { title: 'GET /users/:userId/avatar', method: 'getUserAvatarRaw', api: usersApi, params: { userId } },
    { title: 'GET /user', method: 'getCurrentUserRaw', api: userApi },
    { title: 'PUT /user', method: 'updateCurrentUserRaw', api: userApi, params: {} },
    { title: 'GET /user/avatar', method: 'getCurrentUserAvatarRaw', api: userApi },
    { title: 'GET /user/notifications', method: 'getCurrentUserNotificationsRaw', api: userApi, params: {} },
    { title: 'DELETE /user/notifications', method: 'deleteAllCurrentUserNotificationsRaw', api: userApi },
    {
      title: 'DELETE /user/notifications',
      method: 'deleteCurrentUserNotificationByNotificationIdRaw',
      api: userApi,
      params: { notificationId },
    },
  ];

  if (auth === ANONYMOUS) {
    return { authorizedCases: authorizedCasesForAnonymous, unauthorizedCases: unauthorizedCasesForAnonymous, apiPortalApi };
  }
  return {
    authorizedCases: [...authorizedCasesForAnonymous, ...unauthorizedCasesForAnonymous],
    apiPortalApi,
  };
}

describe('Portal: Authorizations', () => {
  describe('Anonymous requests', () => {
    const { authorizedCases, unauthorizedCases, apiPortalApi } = portalAuthorizationCases({});

    test.each(authorizedCases)('Authorized: $title', ({ api = apiPortalApi, method, params }) => authorized(api[method](params)));

    test.each(unauthorizedCases)('Unauthorized: $title', ({ api = apiPortalApi, method, params }) => unauthorized(api[method](params)));
  });

  describe('User requests', () => {
    const { authorizedCases, apiPortalApi } = portalAuthorizationCases({ auth: SIMPLE_USER });

    test.each(authorizedCases)('Authorized: $title', ({ api = apiPortalApi, method, params }) => authorized(api[method](params)));
  });
});
