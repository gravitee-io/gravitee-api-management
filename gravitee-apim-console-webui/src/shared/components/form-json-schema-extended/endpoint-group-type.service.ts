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
import { Observable, of } from 'rxjs';
import { isEmpty } from 'lodash';
import { map } from 'rxjs/operators';

import { EndpointGroupV4 } from '../../../entities/management-api-v2';
import { extractModelsFromConfig } from './model-extraction.util';

export type EndpointGroupEntry = {
  name: string;
  type: string;
  models: string[];
};

export type ModelGroup = {
  groupName: string;
  models: string[];
};

@Injectable()
export class EndpointGroupTypeService {
  private endpointGroups: EndpointGroupEntry[] = [];

  public setEndpointGroups(groups: EndpointGroupV4[]): void {
    this.endpointGroups = (groups ?? []).map((group) => {
      const models = this.extractModels(group);
      return { name: group.name ?? '', type: group.type, models };
    });
  }

  public filter$(term: string | undefined, type: string): Observable<EndpointGroupEntry[]> {
    const filtered = this.endpointGroups.filter((g) => g.type.startsWith(type));
    if (isEmpty(term)) {
      return of(filtered);
    }
    return of(filtered.filter((g) => g.name.toLowerCase().indexOf(term.toLowerCase()) === 0));
  }

  public getEndpointGroup(name: string, type: string): Observable<EndpointGroupEntry | undefined> {
    return of(this.endpointGroups.find((g) => g.name === name && g.type.startsWith(type)));
  }

  public getModelsForGroup(groupName: string): Observable<string[]> {
    const group = this.endpointGroups.find((g) => g.name === groupName);
    return of(group?.models ?? []);
  }

  public filterModels$(groupName: string, term: string | undefined): Observable<string[]> {
    return this.getModelsForGroup(groupName).pipe(
      map((models) => {
        if (isEmpty(term)) {
          return models;
        }
        return models.filter((m) => m.toLowerCase().indexOf(term.toLowerCase()) === 0);
      }),
    );
  }

  public getModelGroupsByType(type: string): ModelGroup[] {
    return this.endpointGroups
      .filter((g) => g.type.startsWith(type) && g.models.length > 0)
      .map((g) => ({ groupName: g.name, models: g.models }));
  }

  public filterModelGroupsByType$(type: string, term: string | undefined): Observable<ModelGroup[]> {
    const groups = this.getModelGroupsByType(type);
    if (isEmpty(term)) {
      return of(groups);
    }
    const lower = term.toLowerCase();
    const filtered = groups
      .map((g) => ({ groupName: g.groupName, models: g.models.filter((m) => m.toLowerCase().indexOf(lower) === 0) }))
      .filter((g) => g.models.length > 0);
    return of(filtered);
  }

  public findGroupForModel(type: string, modelName: string): string | undefined {
    return this.endpointGroups.find((g) => g.type.startsWith(type) && g.models.includes(modelName))?.name;
  }

  private extractModels(group: EndpointGroupV4): string[] {
    const allModels: string[] = [];

    // Collect models from ALL endpoints in the group
    for (const endpoint of group.endpoints ?? []) {
      allModels.push(...extractModelsFromConfig(endpoint.configuration));
    }

    // Also check shared configuration
    allModels.push(...extractModelsFromConfig(group.sharedConfiguration));

    // Deduplicate
    return [...new Set(allModels)];
  }
}
