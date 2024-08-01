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
import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { uniqueId } from 'lodash';

import { Constants } from '../entities/Constants';
import {
  CreateSharedPolicyGroup,
  UpdateSharedPolicyGroup,
  SharedPolicyGroup,
  SharedPolicyGroupsSortByParam,
  fakeSharedPolicyGroup,
  fakePagedResult,
  PagedResult,
} from '../entities/management-api-v2';

@Injectable({
  providedIn: 'root',
})
export class SharedPolicyGroupsService {
  public sharedPolicyGroups$ = new BehaviorSubject<PagedResult<SharedPolicyGroup>>(
    fakePagedResult([
      fakeSharedPolicyGroup({
        id: 'SEARCH_SPG',
        name: 'Search env flow',
        phase: 'REQUEST',
      }),
    ]),
  );

  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  list(searchQuery?: string, sortBy?: SharedPolicyGroupsSortByParam, page = 1, perPage = 10): Observable<PagedResult<SharedPolicyGroup>> {
    // TODO: implement this method when endpoint is available

    this.sharedPolicyGroups$.next(
      fakePagedResult(
        this.sharedPolicyGroups$.value.data.map((flow) => {
          if (flow.id === 'SEARCH_SPG') {
            return {
              ...flow,
              description: `Search query: ${searchQuery}, sortBy: ${sortBy}, page: ${page}, perPage: ${perPage}`,
            };
          }
          return flow;
        }),
      ),
    );

    return this.sharedPolicyGroups$;
  }

  get(id: string): Observable<SharedPolicyGroup> {
    return of(this.sharedPolicyGroups$.value.data.find((flow) => flow.id === id));
  }

  create(createSharedPolicyGroup: CreateSharedPolicyGroup): Observable<SharedPolicyGroup> {
    const sharedPolicyGroupToCreate = fakeSharedPolicyGroup({
      id: uniqueId(),
      name: createSharedPolicyGroup.name,
      description: createSharedPolicyGroup.description,
      phase: createSharedPolicyGroup.phase,
    });
    this.sharedPolicyGroups$.next(fakePagedResult([...this.sharedPolicyGroups$.value.data, sharedPolicyGroupToCreate]));
    return of(sharedPolicyGroupToCreate);
  }

  update(id: string, updateSharedPolicyGroup: UpdateSharedPolicyGroup): Observable<SharedPolicyGroup> {
    const sharedPolicyGroupToUpdate = this.sharedPolicyGroups$.value.data.find((spg) => spg.id === id);
    const updatedSharedPolicyGroup = {
      ...sharedPolicyGroupToUpdate,
      name: updateSharedPolicyGroup.name,
      description: updateSharedPolicyGroup.description,
    };
    this.sharedPolicyGroups$.next(
      fakePagedResult(this.sharedPolicyGroups$.value.data.map((spg) => (spg.id === id ? updatedSharedPolicyGroup : spg))),
    );
    return of(updatedSharedPolicyGroup);
  }
}
