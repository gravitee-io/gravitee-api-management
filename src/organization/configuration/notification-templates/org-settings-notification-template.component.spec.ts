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
import { HttpTestingController } from '@angular/common/http/testing';

import { OrgSettingsNotificationTemplateComponent } from './org-settings-notification-template.component';

import { OrganizationSettingsModule } from '../organization-settings.module';
import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../../shared/testing';
import { UIRouterState } from '../../../ajs-upgraded-providers';

describe('OrgSettingsNotificationTemplateComponent', () => {
  let fixture: ComponentFixture<OrgSettingsNotificationTemplateComponent>;
  let component: OrgSettingsNotificationTemplateComponent;
  let httpTestingController: HttpTestingController;
  const fakeConstants = CONSTANTS_TESTING;
  const mockUiRouterState = { go: jest.fn() };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioHttpTestingModule, OrganizationSettingsModule],
      providers: [
        {
          provide: 'Constants',
          useValue: fakeConstants,
        },
        { provide: UIRouterState, useValue: mockUiRouterState },
      ],
    });
  });

  beforeEach(() => {
    fakeConstants.org.settings.alert.enabled = true;
    fixture = TestBed.createComponent(OrgSettingsNotificationTemplateComponent);
    component = fixture.componentInstance;
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  it('should setup notificationTemplatesByScope ', async () => {
    expect(component).toBeDefined();
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
