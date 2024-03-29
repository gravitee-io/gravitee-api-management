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

import { OrgSettingsCockpitComponent } from './org-settings-cockpit.component';

import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { OrganizationSettingsModule } from '../organization-settings.module';
import { fakeInstallation } from '../../../entities/installation/installation.fixture';

describe('OrgSettingsCockpitComponent', () => {
  let fixture: ComponentFixture<OrgSettingsCockpitComponent>;
  let component: OrgSettingsCockpitComponent;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, OrganizationSettingsModule],
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(OrgSettingsCockpitComponent);
    component = fixture.componentInstance;

    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  describe('setup properties', () => {
    it('when installation has no Cockpit status', async () => {
      httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/installation`).flush(
        fakeInstallation({
          cockpitURL: 'https://cockpit.gravitee.io',
          additionalInformation: {},
        }),
      );

      expect(component.icon).toEqual('explore');
      expect(component.title).toEqual('Meet Gravitee Cloud...');
      expect(component.message).toEqual(
        'Create an account on <a href="https://cockpit.gravitee.io?utm_source=apim&utm_medium=InApp&utm_campaign=discover_cockpit&utm_term=not_registered" target="_blank">Gravitee Cloud</a>, register your current installation and start creating new organizations and environments!',
      );
    });

    it('when installation has PENDING Cockpit status', async () => {
      httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/installation`).flush(
        fakeInstallation({
          cockpitURL: 'https://cockpit.gravitee.io',
          additionalInformation: {
            COCKPIT_INSTALLATION_STATUS: 'PENDING',
          },
        }),
      );

      expect(component.icon).toEqual('schedule');
      expect(component.title).toEqual('Almost there!');
      expect(component.message).toEqual(
        'Your installation is connected but it still has to be accepted on <a href="https://cockpit.gravitee.io?utm_source=apim&utm_medium=InApp&utm_campaign=discover_cockpit&utm_term=pending" target="_blank">Gravitee Cloud</a>!',
      );
    });

    it('when installation has ACCEPTED Cockpit status', async () => {
      httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/installation`).flush(
        fakeInstallation({
          cockpitURL: 'https://cockpit.gravitee.io',
          additionalInformation: {
            COCKPIT_INSTALLATION_STATUS: 'ACCEPTED',
          },
        }),
      );

      expect(component.icon).toEqual('check_circle');
      expect(component.title).toEqual('Congratulations!');
      expect(component.message).toEqual(
        'Your installation is now connected to <a href="https://cockpit.gravitee.io?utm_source=apim&utm_medium=InApp&utm_campaign=discover_cockpit&utm_term=registered" target="_blank">Gravitee Cloud</a>, you can now explore all the possibilities offered by Gravitee Cloud!',
      );
    });

    it('when installation has REJECTED Cockpit status', async () => {
      httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/installation`).flush(
        fakeInstallation({
          cockpitURL: 'https://cockpit.gravitee.io',
          additionalInformation: {
            COCKPIT_INSTALLATION_STATUS: 'REJECTED',
          },
        }),
      );

      expect(component.icon).toEqual('warning');
      expect(component.title).toEqual('No luck!');
      expect(component.message).toEqual(
        'Seems that your installation is connected to <a href="https://cockpit.gravitee.io?utm_source=apim&utm_medium=InApp&utm_campaign=discover_cockpit&utm_term=rejected" target="_blank">Gravitee Cloud</a>, but has been rejected...',
      );
    });

    it('when installation has DELETED Cockpit status', async () => {
      httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/installation`).flush(
        fakeInstallation({
          cockpitURL: 'https://cockpit.gravitee.io',
          additionalInformation: {
            COCKPIT_INSTALLATION_STATUS: 'DELETED',
          },
        }),
      );

      expect(component.icon).toEqual('gps_off');
      expect(component.title).toEqual('Installation unlinked!');
      expect(component.message).toEqual(
        'Seems that your installation is connected to <a href="https://cockpit.gravitee.io?utm_source=apim&utm_medium=InApp&utm_campaign=discover_cockpit&utm_term=removed" target="_blank">Gravitee Cloud</a>, but is not linked anymore...',
      );
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });
});
