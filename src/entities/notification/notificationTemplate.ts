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

export type NotificationTemplateType = 'EMAIL' | 'PORTAL';

export interface NotificationTemplate {
  /**
   * Be careful with the id, it can exist or not according to template
   * customization. The first time a notification template is fetch it will
   * have no id but all the other fields will be there. After been customized
   * once it will always have an id, whether it has all default values or not
   */
  id?: string;
  hook?: string;
  scope: string;
  name: string;
  description: string;
  title: string;
  content: string;
  type: NotificationTemplateType;
  created_at: number;
  updated_at?: number;
  enabled?: boolean;
}
