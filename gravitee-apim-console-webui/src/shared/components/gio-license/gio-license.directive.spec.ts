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
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { Component, Input } from '@angular/core';

import { GioLicenseOptions } from './gio-license.directive';
import { GioLicenseModule } from './gio-license.module';

import { GioHttpTestingModule } from '../../testing';
import { GioLicenseTestingModule } from '../../testing/gio-license.testing.module';

@Component({ template: `<div [gioLicense]="license" (click)="onClick()">A Content</div>` })
class TestLicenseComponent {
  @Input()
  license: GioLicenseOptions;

  // eslint-disable-next-line @typescript-eslint/no-empty-function
  onClick() {}
}

describe('GioLicenseDirective', () => {
  let fixture: ComponentFixture<TestLicenseComponent>;
  let component: TestLicenseComponent;
  function prepareTestLicenseComponent(licenseOptions: GioLicenseOptions, license: boolean) {
    fixture = TestBed.configureTestingModule({
      declarations: [TestLicenseComponent],
      imports: [GioHttpTestingModule, GioLicenseModule, GioLicenseTestingModule.with(license)],
    }).createComponent(TestLicenseComponent);
    component = fixture.componentInstance;
    component.license = licenseOptions;
    fixture.detectChanges();
  }

  describe('Override click & open dialog', () => {
    it('should override click if license not allowed', () => {
      prepareTestLicenseComponent({ feature: 'apim-custom-roles' }, false);
      const onClickSpy = jest.spyOn(component, 'onClick');
      fixture.detectChanges();

      const element = fixture.nativeElement.querySelector('div');
      element.click();

      expect(onClickSpy).toHaveBeenCalledTimes(0);
    });
  });

  describe('Not override click & not open dialog', () => {
    it('should not override click if license is allowed', () => {
      prepareTestLicenseComponent({ feature: 'apim-custom-roles' }, true);
      const onClickSpy = jest.spyOn(component, 'onClick');
      fixture.detectChanges();

      const element = fixture.nativeElement.querySelector('div');
      element.click();

      expect(onClickSpy).toHaveBeenCalledTimes(1);
    });
  });
});
