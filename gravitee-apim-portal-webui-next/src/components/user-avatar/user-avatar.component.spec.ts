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
import { provideRouter } from '@angular/router';

import { UserAvatarComponent } from './user-avatar.component';
import { fakeUser } from '../../entities/user/user.fixtures';

describe('UserAvatarComponent', () => {
  let fixture: ComponentFixture<UserAvatarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserAvatarComponent, NoopAnimationsModule],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(UserAvatarComponent);
    fixture.componentRef.setInput('user', fakeUser());
    fixture.detectChanges();
  });

  it('should not show Analytics menu item when analyticsEnabled is false', async () => {
    fixture.componentRef.setInput('analyticsEnabled', false);
    fixture.detectChanges();

    (fixture.nativeElement as HTMLElement).querySelector('.user-avatar')?.dispatchEvent(new MouseEvent('click'));
    fixture.detectChanges();
    await fixture.whenStable();

    const panel = document.querySelector('.mat-mdc-menu-panel');
    const labels = Array.from(panel?.querySelectorAll('.mat-mdc-menu-item') ?? []).map(el => el.textContent?.trim());
    expect(labels.some(t => t === 'Analytics')).toBe(false);
  });

  it('should show Analytics menu item when analyticsEnabled is true', async () => {
    fixture.componentRef.setInput('analyticsEnabled', true);
    fixture.detectChanges();

    (fixture.nativeElement as HTMLElement).querySelector('.user-avatar')?.dispatchEvent(new MouseEvent('click'));
    fixture.detectChanges();
    await fixture.whenStable();

    const panel = document.querySelector('.mat-mdc-menu-panel');
    const labels = Array.from(panel?.querySelectorAll('.mat-mdc-menu-item') ?? []).map(el => el.textContent?.trim());
    expect(labels).toContain('Analytics');
  });
});
