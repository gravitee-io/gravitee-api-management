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

import * as _ from 'lodash';

export class ApplicationType {
  public name: string;
  public id: string;
  public description: string;
  public icon: string;
  public oauth?: any;
  public configuration: any;
  public default_grant_types: Array<any>;
  public requires_redirect_uris: boolean;
  public allowed_grant_types: Array<any>;
  public mandatory_grant_types: Array<any>;

  constructor({ name, id, description, requires_redirect_uris, allowed_grant_types, default_grant_types, mandatory_grant_types }) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.requires_redirect_uris = requires_redirect_uris;
    this.allowed_grant_types = allowed_grant_types;
    this.default_grant_types = default_grant_types;
    this.mandatory_grant_types = mandatory_grant_types;
    this.icon = this.getIcon();
  }

  public isOauth() {
    return this.id.toLowerCase() !== 'simple';
  }

  public isGrantTypeMandatory(grantType: { type }): boolean {
    return this.mandatory_grant_types && _.indexOf(this.mandatory_grant_types, grantType.type) !== -1;
  }

  private getIcon() {
    switch (this.id.toUpperCase()) {
      case 'BROWSER':
        return 'computer';
      case 'WEB':
        return 'language';
      case 'NATIVE':
        return 'phone_android';
      case 'BACKEND_TO_BACKEND':
        return 'share';
      default:
        return 'pan_tool';
    }
  }
}
