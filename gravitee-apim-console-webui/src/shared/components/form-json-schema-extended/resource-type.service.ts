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
import { EMPTY, Observable, of } from 'rxjs';
import { isEmpty } from 'lodash';
import { catchError, shareReplay, map, startWith } from 'rxjs/operators';

import { Resource } from '../../../entities/management-api-v2';
import { ResourceService } from '../../../services-ngx/resource.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

export type PlanOAuth2Resource = {
  name: string;
  type: string;
  icon: string;
};

@Injectable()
export class ResourceTypeService extends FieldType<FieldTypeConfig> {
  private resources: Observable<PlanOAuth2Resource[]> = of([]).pipe(startWith([]));

  private listResource$ = this.resourceService.list({ expandSchema: false, expandIcon: true }).pipe(
    catchError((error) => {
      this.snackBarService.error(error.error?.message ?? 'An error occurred while loading resources.');
      return EMPTY;
    }),
    shareReplay(1),
  );

  constructor(
    private readonly resourceService: ResourceService,
    private readonly snackBarService: SnackBarService,
  ) {
    super();
  }

  public setResources(resources: Resource[]) {
    this.resources = this.listResource$.pipe(
      map((resourceTypes) => {
        return [...resources].map((resource) => {
          const resourceType = resourceTypes.find((type) => type.id === resource.type);
          const icon = resourceType?.icon ? resourceType.icon : null;
          return { icon, name: resource.name, type: resource.type };
        });
      }),
    );
  }

  public getResource(name: string, type: string): Observable<PlanOAuth2Resource> {
    return this.resources.pipe(map((resources) => resources.find((resource) => resource.name === name && resource.type.startsWith(type))));
  }

  public filter$(term: string | undefined, type: string): Observable<PlanOAuth2Resource[]> {
    if (!this.resources) {
      return of([]);
    }
    return this.filterResources(term, type);
  }

  private filterResources(term: string | undefined, type: string): Observable<PlanOAuth2Resource[]> {
    if (isEmpty(term)) {
      return this.resources.pipe(map((resources) => resources.filter((resource) => resource.type.startsWith(type))));
    }
    return this.resources.pipe(
      map((resources) =>
        resources.filter((resource) => resource.name.toLowerCase().indexOf(term.toLowerCase()) === 0 && resource.type.startsWith(type)),
      ),
    );
  }
}
