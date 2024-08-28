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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { HttpTestingController } from '@angular/common/http/testing';

import { MenuLinkEditComponent } from './menu-link-edit.component';
import { MenuLinkEditHarness } from './menu-link-edit.harness';

import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { CONSTANTS_TESTING, GioTestingModule } from '../../../../shared/testing';
import { fakePortalMenuLink } from '../../../../entities/management-api-v2';

describe('MenuLinkEditComponent', () => {
  const menuLinkId = 'portal-menu-link';
  const fakeSnackBarService = {
    success: jest.fn(),
  };

  let fixture: ComponentFixture<MenuLinkEditComponent>;
  let componentHarness: MenuLinkEditHarness;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, MenuLinkEditComponent, GioTestingModule],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { menuLinkId } } } },
        { provide: SnackBarService, useValue: fakeSnackBarService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MenuLinkEditComponent);
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, MenuLinkEditHarness);
    httpTestingController = TestBed.inject(HttpTestingController);

    httpTestingController
      .expectOne(`${CONSTANTS_TESTING.env.v2BaseURL}/ui/portal-menu-links/${menuLinkId}`)
      .flush(fakePortalMenuLink({ id: menuLinkId }));

    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('form should be filled', async () => {
    expect(await componentHarness.getName()).toEqual('link name');
    expect(await componentHarness.getTarget()).toEqual('link target');
  });

  it('should fill form and submit', async () => {
    await componentHarness.setName('name');
    await componentHarness.reset();
    expect(await componentHarness.getName()).toStrictEqual('link name');

    await componentHarness.setName('name');
    await componentHarness.setTarget('target');
    await componentHarness.submit();

    const req = httpTestingController.expectOne({
      method: 'PUT',
      url: `${CONSTANTS_TESTING.env.v2BaseURL}/ui/portal-menu-links/${menuLinkId}`,
    });
    const updatedLink = {
      name: 'name',
      target: 'target',
      visibility: 'PRIVATE',
      order: 1,
    };
    expect(req.request.body).toEqual(updatedLink);

    req.flush(fakePortalMenuLink({ id: menuLinkId, name: 'name', target: 'target' }));

    expect(await componentHarness.getName()).toStrictEqual('name');
    expect(await componentHarness.getTarget()).toStrictEqual('target');
    expect(fakeSnackBarService.success).toHaveBeenCalledWith("Menu link 'name' updated successfully");
  });

  it('should not be able to save', async () => {
    await componentHarness.setName('name');
    await componentHarness.setTarget(null);
    expect(await componentHarness.isSubmitInvalid()).toBeTruthy();
  });
});
