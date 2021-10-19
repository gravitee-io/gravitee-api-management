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
export interface UserDetails {
  id: string;
  email: string;
  firstname: string;
  lastname: string;
  source: string;
  sourceId: string;
  isPrimaryOwner: boolean;
  roles: any[];
  groupsByEnvironment: Record<string, string[]>;
  username: string;
  picture: any[];
  firstLogin: boolean;
  displayNewsletterSubscription: boolean;
  customFields: Record<string, any>;
  created_at: number;
  updatedAt: number;
  lastConnectionAt: number;
}
