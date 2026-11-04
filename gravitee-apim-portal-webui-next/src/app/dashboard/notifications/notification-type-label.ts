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

// Hooks of the API, API Product, application and portal notifiers (ApiHook, ApiProductHook, ApplicationHook, PortalHook in the REST API).
const NOTIFICATION_TYPE_LABELS: Record<string, string> = {
  APIKEY_EXPIRED: $localize`:@@notificationTypeApiKeyExpired:API key expired`,
  APIKEY_RENEWED: $localize`:@@notificationTypeApiKeyRenewed:API key renewed`,
  APIKEY_REVOKED: $localize`:@@notificationTypeApiKeyRevoked:API key revoked`,
  SUBSCRIPTION_NEW: $localize`:@@notificationTypeSubscriptionNew:New subscription`,
  SUBSCRIPTION_ACCEPTED: $localize`:@@notificationTypeSubscriptionAccepted:Subscription accepted`,
  SUBSCRIPTION_CLOSED: $localize`:@@notificationTypeSubscriptionClosed:Subscription closed`,
  SUBSCRIPTION_PAUSED: $localize`:@@notificationTypeSubscriptionPaused:Subscription paused`,
  SUBSCRIPTION_RESUMED: $localize`:@@notificationTypeSubscriptionResumed:Subscription resumed`,
  SUBSCRIPTION_REJECTED: $localize`:@@notificationTypeSubscriptionRejected:Subscription rejected`,
  SUBSCRIPTION_TRANSFERRED: $localize`:@@notificationTypeSubscriptionTransferred:Subscription transferred`,
  SUBSCRIPTION_FAILED: $localize`:@@notificationTypeSubscriptionFailed:Subscription failed`,
  NEW_SUPPORT_TICKET: $localize`:@@notificationTypeNewSupportTicket:New support ticket`,
  API_STARTED: $localize`:@@notificationTypeApiStarted:API started`,
  API_STOPPED: $localize`:@@notificationTypeApiStopped:API stopped`,
  API_UPDATED: $localize`:@@notificationTypeApiUpdated:API updated`,
  API_DEPLOYED: $localize`:@@notificationTypeApiDeployed:API deployed`,
  API_DEPRECATED: $localize`:@@notificationTypeApiDeprecated:API deprecated`,
  API_PRODUCT_DEPLOYED: $localize`:@@notificationTypeApiProductDeployed:API Product deployed`,
  API_PRODUCT_UPDATED: $localize`:@@notificationTypeApiProductUpdated:API Product updated`,
  NEW_RATING: $localize`:@@notificationTypeNewRating:New rating`,
  NEW_RATING_ANSWER: $localize`:@@notificationTypeNewRatingAnswer:New rating answer`,
  MESSAGE: $localize`:@@notificationTypeMessage:Message`,
  ASK_FOR_REVIEW: $localize`:@@notificationTypeAskForReview:Review requested`,
  REVIEW_OK: $localize`:@@notificationTypeReviewOk:Review accepted`,
  REQUEST_FOR_CHANGES: $localize`:@@notificationTypeRequestForChanges:Changes requested`,
  NEW_SPEC_GENERATED: $localize`:@@notificationTypeNewSpecGenerated:New specification generated`,
  USER_REGISTRATION_REQUEST: $localize`:@@notificationTypeUserRegistrationRequest:User registration request`,
  USER_REGISTERED: $localize`:@@notificationTypeUserRegistered:User registered`,
  USER_CREATED: $localize`:@@notificationTypeUserCreated:User created`,
  USER_FIRST_LOGIN: $localize`:@@notificationTypeUserFirstLogin:User first login`,
  PASSWORD_RESET: $localize`:@@notificationTypePasswordReset:Password reset`,
  FEDERATED_APIS_INGESTION_COMPLETE: $localize`:@@notificationTypeFederatedApisIngestionComplete:Federated APIs ingestion complete`,
  GROUP_INVITATION: $localize`:@@notificationTypeGroupInvitation:Group invitation`,
};

const DEFAULT_NOTIFICATION_TYPE_LABEL = $localize`:@@notificationTypeDefault:Notification`;

export function deriveNotificationTypeLabel(hook: string | undefined): string {
  return (hook && NOTIFICATION_TYPE_LABELS[hook]) || DEFAULT_NOTIFICATION_TYPE_LABEL;
}
