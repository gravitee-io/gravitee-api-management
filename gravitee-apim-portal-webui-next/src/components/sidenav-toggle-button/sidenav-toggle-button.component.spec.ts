/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SidenavToggleButtonComponent } from './sidenav-toggle-button.component';
import { SidenavToggleButtonComponentHarness } from './sidenav-toggle-button.component.harness';

describe('SidenavToggleButtonComponent', () => {
  let fixture: ComponentFixture<SidenavToggleButtonComponent>;
  let harness: SidenavToggleButtonComponentHarness;

  const init = async (collapsed = false) => {
    await TestBed.configureTestingModule({
      imports: [SidenavToggleButtonComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SidenavToggleButtonComponent);
    fixture.componentRef.setInput('collapsed', collapsed);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, SidenavToggleButtonComponentHarness);
  };

  it('should display right arrow for collapsed state', async () => {
    await init(true);

    const icon = await harness.getIcon();
    expect(icon).toBeDefined();
    expect(await icon.text()).toEqual('keyboard_double_arrow_right');
  });

  it('should display left arrow for expanded state', async () => {
    await init(false);

    const icon = await harness.getIcon();
    expect(icon).toBeDefined();
    expect(await icon.text()).toEqual('keyboard_double_arrow_left');
  });

  it('should emit toggle event when clicked', async () => {
    await init();

    const toggleState = jest.spyOn(fixture.componentInstance.toggleState, 'emit');

    const button = await harness.getButton();
    expect(button).toBeDefined();

    await button.click();
    expect(toggleState).toHaveBeenCalled();
  });
});
