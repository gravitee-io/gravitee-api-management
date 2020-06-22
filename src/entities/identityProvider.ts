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
export class GroupMapping {
  public condition: string;
  public groups: string[];
}

export class RoleMapping {
  public condition: string;
  public environments: string[];
  public organizations: string[];
}

export class IdentityProvider {
  public id: string;
  public name: string;
  public description: string;
  public enabled: boolean;
  public type: string;
  public configuration: Map<string, any>;
  public groupMappings: GroupMapping[];
  public roleMappings: RoleMapping[];
  public userProfileMapping: { id: string, firstname: string, lastname: string, email: string, picture: string };
  public emailRequired: boolean;
  public scopes: any;
  public scope: any;
  public userLogoutEndpoint: any;
  public syncMappings: boolean;

  constructor() {
    'ngInject';
  }
}

export class IdentityProviderActivation {
  public identityProvider: string;
  public referenceId?: string;
  public referenceType?: string;

  constructor() {
    'ngInject';
  }
}
