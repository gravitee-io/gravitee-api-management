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
import { PortalNotification } from '../../../entities/notification/portal-notification';

export type PortalNotificationSource = 'API' | 'APPLICATION' | 'PORTAL';

export interface EnrichedPortalNotification {
  id: string;
  created_at?: string;
  title?: string;
  message: string;
  notificationType: string;
  resourceName: string;
  source: PortalNotificationSource;
  apiName?: string;
  applicationName?: string;
  read: boolean;
}

const TITLE_PREFIX = /^\[([^\]]+)]\s*(.*)$/;
const API_NAME_IN_MESSAGE = /(?:of the api|api)\s+"([^"]+)"/i;
const API_PRODUCT_NAME_IN_MESSAGE = /API Product\s+"([^"]+)"/i;
const APPLICATION_NAME_IN_MESSAGE = /application\s+"([^"]+)"/i;
const API_HOOK_MESSAGE = /(?:for the application\s+")|(?:application\s+"[^"]+"\s+request a subscription)|(?:apikey has )/i;
const APPLICATION_HOOK_MESSAGE = /of the (?:api|API Product)\s+"/i;
const API_LIFECYCLE_TYPE = /^(api[-\s]|apikey|ask for api|accept api|reject api|new rating|new specification)/i;
const API_LIFECYCLE_MESSAGE = /was (?:started|stopped|updated|deployed|deprecated)/i;

export function enrichPortalNotification(notification: PortalNotification, read = false): EnrichedPortalNotification {
  const title = (notification.title ?? '').trim();
  const message = (notification.message ?? '').replace(/\s+/g, ' ').trim();
  const prefix = title.match(TITLE_PREFIX);
  const bracketName = prefix?.[1]?.trim();
  const notificationType = (prefix?.[2]?.trim() || title || $localize`:@@notificationsFallbackType:Notification`).trim();

  const apiFromMessage = firstMatch(message, API_NAME_IN_MESSAGE) ?? firstMatch(message, API_PRODUCT_NAME_IN_MESSAGE);
  const applicationFromMessage = firstMatch(message, APPLICATION_NAME_IN_MESSAGE);

  const source = resolveSource(notificationType, message, bracketName, apiFromMessage, applicationFromMessage);
  const apiName = resolveApiName(source, bracketName, apiFromMessage, message);
  const applicationName = resolveApplicationName(source, bracketName, applicationFromMessage);
  const resourceName = (source === 'APPLICATION' ? applicationName : source === 'API' ? apiName : bracketName) ?? '';

  return {
    id: notification.id ?? '',
    created_at: notification.created_at,
    title,
    message,
    notificationType,
    resourceName: resourceName || $localize`:@@notificationsUnknownResource:—`,
    source,
    apiName,
    applicationName,
    read,
  };
}

function resolveSource(
  notificationType: string,
  message: string,
  bracketName: string | undefined,
  apiFromMessage: string | undefined,
  applicationFromMessage: string | undefined,
): PortalNotificationSource {
  if (API_HOOK_MESSAGE.test(message) || API_LIFECYCLE_TYPE.test(notificationType) || API_LIFECYCLE_MESSAGE.test(message)) {
    return 'API';
  }
  if (APPLICATION_HOOK_MESSAGE.test(message)) {
    return 'APPLICATION';
  }
  if (bracketName && apiFromMessage && bracketName === apiFromMessage) {
    return 'API';
  }
  if (bracketName && applicationFromMessage && bracketName === applicationFromMessage) {
    return 'APPLICATION';
  }
  if (bracketName && applicationFromMessage) {
    return 'API';
  }
  if (bracketName && apiFromMessage) {
    return 'APPLICATION';
  }
  if (apiFromMessage || bracketName) {
    return 'API';
  }
  if (applicationFromMessage) {
    return 'APPLICATION';
  }
  return 'PORTAL';
}

function resolveApiName(
  source: PortalNotificationSource,
  bracketName: string | undefined,
  apiFromMessage: string | undefined,
  message: string,
): string | undefined {
  if (apiFromMessage) {
    return apiFromMessage;
  }
  if (source === 'API') {
    return bracketName ?? firstQuotedValue(message);
  }
  return undefined;
}

function resolveApplicationName(
  source: PortalNotificationSource,
  bracketName: string | undefined,
  applicationFromMessage: string | undefined,
): string | undefined {
  if (applicationFromMessage) {
    return applicationFromMessage;
  }
  if (source === 'APPLICATION') {
    return bracketName;
  }
  return undefined;
}

function firstMatch(value: string, pattern: RegExp): string | undefined {
  const match = value.match(pattern);
  return match?.[1]?.trim() || undefined;
}

function firstQuotedValue(value: string): string | undefined {
  const match = value.match(/"([^"]+)"/);
  return match?.[1]?.trim() || undefined;
}
