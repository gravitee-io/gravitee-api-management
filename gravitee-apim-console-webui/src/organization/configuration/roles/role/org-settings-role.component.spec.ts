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

import { OrgSettingsRoleComponent } from './org-settings-role.component';

import { OrganizationSettingsModule } from '../../organization-settings.module';
import { GioHttpTestingModule } from '../../../../shared/testing';

describe('OrgSettingsRoleComponent', () => {
  let fixture: ComponentFixture<OrgSettingsRoleComponent>;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, OrganizationSettingsModule],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(OrgSettingsRoleComponent);
  });

  it('should init the component', () => {
    expect(fixture.componentInstance).toBeDefined();
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
