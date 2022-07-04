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
import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { FilteredCatalogComponent } from './filtered-catalog.component';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ApiStatesPipe } from '../../../pipes/api-states.pipe';
import { ApiLabelsPipe } from '../../../pipes/api-labels.pipe';
import { Api, ApiService, ApisResponse, User } from '../../../../../projects/portal-webclient-sdk/src/lib';
import { of } from 'rxjs';
import { ConfigurationService } from '../../../services/configuration.service';

describe('FilteredCatalogComponent', () => {
  const category = 'cat';
  const admin: User = { id: 'admin' };
  const api1: Api = { id: 'api#1', name: 'api1', description: 'description', version: '1', owner: admin };
  const api2: Api = { id: 'api#2', name: 'api2', description: 'description', version: '1', owner: admin };
  const apisResponse: ApisResponse = { data: [api1, api2] };

  const createComponent = createComponentFactory({
    component: FilteredCatalogComponent,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    imports: [HttpClientTestingModule, RouterTestingModule],
    declarations: [ApiStatesPipe, ApiLabelsPipe],
    providers: [
      ApiStatesPipe,
      ApiLabelsPipe,
      mockProvider(ConfigurationService, {
        hasFeature: () => true,
      }),
    ],
  });

  let spectator: Spectator<FilteredCatalogComponent>;
  let component: FilteredCatalogComponent;

  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load the apis and the promoted api', async () => {
    expect.assertions(4);

    spectator.component.currentCategory = category;
    const apiServiceSpy = jest.spyOn(spectator.inject(ApiService), 'getApis').mockReturnValue(of(apisResponse) as any);

    await spectator.component._load();

    expect(apiServiceSpy).toHaveBeenCalledTimes(3);

    // should load all others apis
    expect(apiServiceSpy).toHaveBeenCalledWith({
      category,
      filter: null,
      promoted: false,
      page: 1,
      size: 6,
    });

    // should load random list
    expect(apiServiceSpy).toHaveBeenCalledWith({
      filter2: undefined,
      size: 4,
    });

    // should load promoted api of the category
    expect(apiServiceSpy).toHaveBeenCalledWith({
      category,
      filter: null,
      promoted: true,
      size: 1,
    });
  });
});
