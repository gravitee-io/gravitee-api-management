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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';
import { InteractivityChecker } from '@angular/cdk/a11y';

import { PolicyStudioPropertiesComponent } from './policy-studio-properties.component';
import { PolicyStudioPropertiesModule } from './policy-studio-properties.module';

import { GioHttpTestingModule } from '../../../../shared/testing';
import { fakeApi } from '../../../../entities/api/Api.fixture';
import { User } from '../../../../entities/user';
import { PolicyStudioService } from '../policy-studio.service';
import { toApiDefinition } from '../models/ApiDefinition';

describe('PolicyStudioPropertiesComponent', () => {
  let fixture: ComponentFixture<PolicyStudioPropertiesComponent>;
  let component: PolicyStudioPropertiesComponent;
  let httpTestingController: HttpTestingController;
  let policyStudioService: PolicyStudioService;

  const currentUser = new User();
  currentUser.userApiPermissions = ['api-plan-r', 'api-plan-u'];

  const api = fakeApi();

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, PolicyStudioPropertiesModule],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(PolicyStudioPropertiesComponent);
    component = fixture.componentInstance;

    httpTestingController = TestBed.inject(HttpTestingController);
    policyStudioService = TestBed.inject(PolicyStudioService);
    policyStudioService.emitApiDefinition(toApiDefinition(api));

    fixture.detectChanges();
  });

  describe('ngOnInit', () => {
    it('should setup properties', async () => {
      expect(component.apiDefinition).toStrictEqual({
        id: api.id,
        name: api.name,
        flows: api.flows,
        flow_mode: api.flow_mode,
        resources: api.resources,
        plans: api.plans,
        version: api.version,
        services: api.services,
        properties: api.properties,
      });
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
