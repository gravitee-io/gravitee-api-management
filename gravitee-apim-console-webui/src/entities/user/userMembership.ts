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

export interface UserMembership<T extends 'application' | 'api' | string = string> {
  memberships: UserMembershipMemberships[];
  metadata: T extends 'application'
    ? UserMembershipMetadataApplication
    : T extends 'api'
    ? UserMembershipMetadataApi
    : UserMembershipMetadata;
}
export interface UserMembershipMemberships {
  reference: string;
  type: string;
  roles: Record<string, string>;
  source: string;
}

export type UserMembershipMetadata = Record<string, Record<string, any>>;

export type UserMembershipMetadataApplication = Record<
  string,
  {
    name: string;
  }
>;

export type UserMembershipMetadataApi = Record<
  string,
  {
    name: string;
    version?: string;
    visibility?: 'PUBLIC' | 'PRIVATE';
  }
>;
