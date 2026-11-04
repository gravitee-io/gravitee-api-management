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
import { Links } from '../pagination/links';

/** An API or application referenced by a notification. `name` is absent when the resource has been deleted. */
export interface PortalNotificationReference {
  id: string;
  name?: string | null;
}

export interface PortalNotification {
  id: string;
  title: string;
  message: string;
  created_at: string;
  read: boolean;
  hook?: string;
  api?: PortalNotificationReference;
  application?: PortalNotificationReference;
}

export interface PortalNotificationsPagination {
  current_page?: number;
  size?: number;
  total?: number;
  total_pages?: number;
  first?: number;
  last?: number;
}

export interface PortalNotificationsResponse {
  data: PortalNotification[];
  metadata?: {
    data?: { total?: number };
    pagination?: PortalNotificationsPagination;
  };
  links?: Links;
}

export interface PortalNotificationFilters {
  apis: PortalNotificationReference[];
  applications: PortalNotificationReference[];
}

export interface PortalNotificationListParams {
  page: number;
  size: number;
  read?: boolean;
  apiId?: string;
  applicationId?: string;
}
