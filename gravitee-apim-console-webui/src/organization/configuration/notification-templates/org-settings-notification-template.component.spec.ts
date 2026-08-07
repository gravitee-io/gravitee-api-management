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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { HarnessLoader } from '@angular/cdk/testing';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSlideToggleHarness } from '@angular/material/slide-toggle/testing';
import { GioMonacoEditorHarness, GioSaveBarHarness } from '@gravitee/ui-particles-angular';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';

import { OrgSettingsNotificationTemplateComponent } from './org-settings-notification-template.component';

import { OrganizationSettingsModule } from '../organization-settings.module';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../shared/testing';
import { fakeAlertStatus } from '../../../entities/alerts/alertStatus.fixture';
import { fakeNotificationTemplate } from '../../../entities/notification/notificationTemplate.fixture';
import { Constants } from '../../../entities/Constants';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

describe('OrgSettingsNotificationTemplateComponent', () => {
  let fixture: ComponentFixture<OrgSettingsNotificationTemplateComponent>;
  let component: OrgSettingsNotificationTemplateComponent;
  let httpTestingController: HttpTestingController;
  let loader: HarnessLoader;
  let routeParams: { hook: string; scope: string };
  let snackBarService: { error: jest.Mock; success: jest.Mock };
  let routerNavigateSpy: jest.SpyInstance;

  beforeEach(() => {
    routeParams = { hook: 'hook', scope: 'scope' };
    snackBarService = { error: jest.fn(), success: jest.fn() };

    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, OrganizationSettingsModule],
      providers: [
        {
          provide: Constants,
          useValue: CONSTANTS_TESTING,
        },
        {
          provide: SnackBarService,
          useValue: snackBarService,
        },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              get params() {
                return routeParams;
              },
            },
            fragment: of(''),
          },
        },
      ],
    });

    routerNavigateSpy = jest.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  function createComponent() {
    fixture = TestBed.createComponent(OrgSettingsNotificationTemplateComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);

    component = fixture.componentInstance;
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  it('should setup view models', async () => {
    createComponent();
    respondToAlertRequest(fakeAlertStatus({ available_plugins: 2 }));
    respondToNotificationTemplatesRequest([fakeNotificationTemplate({ name: 'Name 1' })]);

    expect(component.hasAlertingPlugin).toBeTruthy();
    expect(component.notificationTemplateName).toEqual('Name 1');
  });

  describe('with classic templates', () => {
    it('should activate, update content and update a template', async () => {
      createComponent();
      respondToAlertRequest(fakeAlertStatus({ available_plugins: 2 }));
      const baseNotificationTemplate = fakeNotificationTemplate({
        name: 'Name 1',
        enabled: false,
        title: 'Title 1',
        content: '<html>Content</html>',
      });
      respondToNotificationTemplatesRequest([baseNotificationTemplate]);

      const titleInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=title]' }));
      const contentInput = await loader.getHarness(GioMonacoEditorHarness.with({ selector: '[formControlName=content]' }));
      const useCustomTemplateToggle = await loader.getHarness(
        MatSlideToggleHarness.with({ selector: '[formControlName=useCustomTemplate]' }),
      );
      const saveBar = await loader.getHarness(GioSaveBarHarness);

      expect(await titleInput.isDisabled()).toBeTruthy();
      expect(await contentInput.isDisabled()).toBeTruthy();
      expect(await useCustomTemplateToggle.isChecked()).toBeFalsy();
      expect(await saveBar.isVisible()).toBeFalsy();

      await useCustomTemplateToggle.toggle();

      expect(await titleInput.isDisabled()).toBeFalsy();
      expect(await titleInput.isRequired()).toBeTruthy();
      expect(await contentInput.isDisabled()).toBeFalsy();

      await titleInput.setValue('New Title');
      await contentInput.setValue('<html>New Content</html>');

      await saveBar.clickSubmit();

      const expectedNotificationTemplate = {
        ...baseNotificationTemplate,
        enabled: true,
        title: 'New Title',
        content: '<html>New Content</html>',
      };
      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.org.baseURL}/configuration/notification-templates/${baseNotificationTemplate.id}`,
      );
      expect(req.request.method).toEqual('PUT');
      expect(req.request.body).toEqual(expectedNotificationTemplate);
      req.flush(expectedNotificationTemplate);

      respondToAlertRequest();
      respondToNotificationTemplatesRequest();
    });

    it('should activate, update content and create a template', async () => {
      createComponent();
      respondToAlertRequest(fakeAlertStatus({ available_plugins: 2 }));
      const baseNotificationTemplate = fakeNotificationTemplate({
        // Remove ID to simulate template creation
        id: undefined,
        name: 'Name 1',
        enabled: false,
        title: 'Title 1',
        content: '<html>Content</html>',
      });

      respondToNotificationTemplatesRequest([baseNotificationTemplate]);

      const titleInput = await loader.getHarness(MatInputHarness.with({ selector: '[formControlName=title]' }));
      const contentInput = await loader.getHarness(GioMonacoEditorHarness.with({ selector: '[formControlName=content]' }));
      const useCustomTemplateToggle = await loader.getHarness(
        MatSlideToggleHarness.with({ selector: '[formControlName=useCustomTemplate]' }),
      );
      const saveBar = await loader.getHarness(GioSaveBarHarness);

      expect(await titleInput.isDisabled()).toBeTruthy();
      expect(await contentInput.isDisabled()).toBeTruthy();
      expect(await useCustomTemplateToggle.isChecked()).toBeFalsy();
      expect(await saveBar.isVisible()).toBeFalsy();

      await useCustomTemplateToggle.toggle();

      expect(await titleInput.isDisabled()).toBeFalsy();
      expect(await titleInput.isRequired()).toBeTruthy();
      expect(await contentInput.isDisabled()).toBeFalsy();

      await titleInput.setValue('New Title');
      await contentInput.setValue('<html>New Content</html>');

      await saveBar.clickSubmit();

      const expectedNotificationTemplate = {
        ...baseNotificationTemplate,
        enabled: true,
        title: 'New Title',
        content: '<html>New Content</html>',
      };
      const req = httpTestingController.expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/notification-templates`);
      expect(req.request.method).toEqual('POST');
      expect(req.request.body).toEqual(expectedNotificationTemplate);
      req.flush(expectedNotificationTemplate);

      respondToAlertRequest();
      respondToNotificationTemplatesRequest();
    });
  });

  describe('with specific "template to include" templates', () => {
    beforeEach(() => {
      // For templates to include the list screen puts the template name in the `hook` route param
      routeParams = { hook: 'header.html', scope: 'TEMPLATES_TO_INCLUDE' };
    });

    it('should only display the template matching the name in the route', async () => {
      createComponent();
      respondToAlertRequest();
      respondToNotificationTemplatesRequest(
        [fakeTemplateToInclude({ name: 'footer.html' }), fakeTemplateToInclude({ name: 'header.html' })],
        'scope=TEMPLATES_TO_INCLUDE&hook=',
      );
      fixture.detectChanges();

      expect(component.notificationTemplateName).toEqual('header.html');
      expect(component.notificationTemplates.map(({ name }) => name)).toEqual(['header.html']);

      const contentInputs = await loader.getAllHarnesses(GioMonacoEditorHarness.with({ selector: '[formControlName=content]' }));
      expect(contentInputs.length).toEqual(1);
      expect(await contentInputs[0].getValue()).toEqual('<html>header.html</html>');
    });

    it('should redirect to the list when the template name in the route does not exist', () => {
      createComponent();
      respondToAlertRequest();
      respondToNotificationTemplatesRequest([fakeTemplateToInclude({ name: 'footer.html' })], 'scope=TEMPLATES_TO_INCLUDE&hook=');
      fixture.detectChanges();

      expect(snackBarService.error).toHaveBeenCalledWith('Notification template "header.html" does not exist.');
      expect(routerNavigateSpy).toHaveBeenCalledWith(['../..'], { relativeTo: expect.anything() });
      expect(component.isLoading).toBe(true);
      expect(fixture.nativeElement.querySelector('form')).toBeNull();
    });

    it('should activate, update content and save a template', async () => {
      createComponent();
      respondToAlertRequest();
      const baseNotificationTemplate = fakeNotificationTemplate({
        content: '<img src="images/GRAVITEE_LOGO.png" width="200" alt="Logo" />',
        created_at: 1634278951703,
        enabled: false,
        hook: '',
        id: '89116cf8-b360-4068-916c-f8b360d06835',
        name: 'header.html',
        scope: 'TEMPLATES_TO_INCLUDE',
        title: '',
        type: 'EMAIL',
      });
      respondToNotificationTemplatesRequest([baseNotificationTemplate], 'scope=TEMPLATES_TO_INCLUDE&hook=');
      fixture.detectChanges();

      // Title form control should not be in the DOM
      expect(fixture.nativeElement.querySelector('[formControlName=title]')).toBeNull();

      const contentInput = await loader.getHarness(GioMonacoEditorHarness.with({ selector: '[formControlName=content]' }));
      const useCustomTemplateToggle = await loader.getHarness(
        MatSlideToggleHarness.with({ selector: '[formControlName=useCustomTemplate]' }),
      );
      const saveBar = await loader.getHarness(GioSaveBarHarness);

      expect(await contentInput.isDisabled()).toBeTruthy();
      expect(await useCustomTemplateToggle.isChecked()).toBeFalsy();
      expect(await saveBar.isVisible()).toBeFalsy();

      await useCustomTemplateToggle.toggle();

      expect(await contentInput.isDisabled()).toBeFalsy();

      await contentInput.setValue('<html>New Content</html>');

      await saveBar.clickSubmit();

      const expectedNotificationTemplate = {
        ...baseNotificationTemplate,
        enabled: true,
        content: '<html>New Content</html>',
      };
      const req = httpTestingController.expectOne(
        `${CONSTANTS_TESTING.org.baseURL}/configuration/notification-templates/${baseNotificationTemplate.id}`,
      );
      expect(req.request.method).toEqual('PUT');
      expect(req.request.body).toEqual(expectedNotificationTemplate);
      req.flush(expectedNotificationTemplate);

      respondToAlertRequest();
      respondToNotificationTemplatesRequest([baseNotificationTemplate], 'scope=TEMPLATES_TO_INCLUDE&hook=');
    });
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  function respondToAlertRequest(alertStatus = fakeAlertStatus()) {
    httpTestingController.expectOne(`${CONSTANTS_TESTING.env.baseURL}/platform/alerts/status`).flush(alertStatus);
  }

  function respondToNotificationTemplatesRequest(
    notificationTemplates = [fakeNotificationTemplate()],
    searchParams = 'scope=scope&hook=hook',
  ) {
    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.org.baseURL}/configuration/notification-templates?${searchParams}`)
      .flush(notificationTemplates);
  }

  function fakeTemplateToInclude(attributes: { name: string }) {
    return fakeNotificationTemplate({
      ...attributes,
      content: `<html>${attributes.name}</html>`,
      hook: '',
      scope: 'TEMPLATES_TO_INCLUDE',
      title: '',
      type: 'EMAIL',
    });
  }
});
