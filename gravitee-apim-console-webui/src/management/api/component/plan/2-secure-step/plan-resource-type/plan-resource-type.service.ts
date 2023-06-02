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
import { Injectable } from '@angular/core';
import { FieldType, FieldTypeConfig } from '@ngx-formly/core';
import { Observable, of } from 'rxjs';
import { isEmpty } from 'lodash';

import { ResourceListItem } from '../../../../../../entities/resource/resourceListItem';
import { Resource } from '../../../../../../entities/management-api-v2';

export type PlanOAuth2Resource = {
  name: string;
  type: string;
  icon: string;
};

@Injectable()
export class PlanResourceTypeService extends FieldType<FieldTypeConfig> {
  private resources: PlanOAuth2Resource[] = [];

  public setResources(resources: Resource[], resourceTypes: ResourceListItem[]) {
    this.resources = [...resources].map((resource) => {
      const resourceType = resourceTypes.find((type) => type.id === resource.type);
      const icon = resourceType?.icon ? resourceType.icon : null;
      return { icon, name: resource.name, type: resource.type };
    });
  }

  public filter$(term: string | undefined, type: string): Observable<PlanOAuth2Resource[]> {
    return of(this.filterResources(term, type));
  }

  private filterResources(term: string | undefined, type: string): PlanOAuth2Resource[] {
    if (isEmpty(term)) {
      return this.resources.filter((resource) => resource.type.startsWith(type));
    }
    return this.resources.filter(
      (resource) => resource.name.toLowerCase().indexOf(term.toLowerCase()) === 0 && resource.type.startsWith(type),
    );
  }
}
