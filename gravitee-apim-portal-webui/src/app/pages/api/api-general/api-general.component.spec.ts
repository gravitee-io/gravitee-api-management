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
import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TestBed } from '@angular/core/testing';

import { ApiGeneralComponent } from './api-general.component';

describe('ApiGeneralComponent', () => {
  const createComponent = createComponentFactory({
    component: ApiGeneralComponent,
    imports: [HttpClientTestingModule, RouterTestingModule, FormsModule, ReactiveFormsModule],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
  });

  let spectator: Spectator<ApiGeneralComponent>;
  let component: ApiGeneralComponent;
  let router: Router;

  beforeEach(() => {
    spectator = createComponent();
    component = spectator.component;
    component.apiHomepage = null;
    router = TestBed.inject(Router);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('goToSearch', () => {
    it('should navigateByUrl with built urlTree', () => {
      const navigateByUrlSpy = jest.spyOn(router, 'navigateByUrl');

      const myTestUrlTree = {};
      jest.spyOn(component as any, 'getSearchUrlTree').mockReturnValue(myTestUrlTree);

      component.goToSearch('labels', 'TheLabel');

      expect(navigateByUrlSpy).toHaveBeenCalledWith(myTestUrlTree);
    });
  });

  describe('getSearchUrl', () => {
    it('should return the search url', () => {
      const resultUrl = component.getSearchUrl('labels', 'myTestValue');

      expect(resultUrl).toBe('/catalog/search?q=labels:%22myTestValue%22');
    });
  });
});
