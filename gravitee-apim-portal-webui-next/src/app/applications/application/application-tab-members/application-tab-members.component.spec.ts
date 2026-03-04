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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ApplicationTabMembersComponent } from './application-tab-members.component';
import { fakeUserApplicationPermissions } from '../../../../entities/permission/permission.fixtures';
import { AppTestingModule } from '../../../../testing/app-testing.module';

describe('ApplicationTabMembersComponent', () => {
  let fixture: ComponentFixture<ApplicationTabMembersComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationTabMembersComponent, NoopAnimationsModule, AppTestingModule],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ApplicationTabMembersComponent);
    fixture.componentRef.setInput('applicationId', 'app-id');
    fixture.componentRef.setInput('userApplicationPermissions', fakeUserApplicationPermissions({ MEMBER: ['R'] }));
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render members section', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="application-tab-members"]')).toBeTruthy();
  });

  it('should display members title', () => {
    const el: HTMLElement = fixture.nativeElement;
    const header = el.querySelector('.application-tab-members__header');
    expect(header?.textContent).toContain('Members');
  });
});
