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
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { EMPTY } from 'rxjs';

import { UserAvatarComponent } from './user-avatar.component';
import { fakeUser } from '../../entities/user/user.fixtures';
import { UserNotificationInboxService } from '../../services/user-notification-inbox.service';

describe('UserAvatarComponent', () => {
  let fixture: ComponentFixture<UserAvatarComponent>;
  const unreadNotificationCount = signal(0);

  function menuLabels(): Array<string | undefined> {
    const panel = document.querySelector('.mat-mdc-menu-panel');
    return Array.from(panel?.querySelectorAll('.mat-mdc-menu-item') ?? []).map(el => el.textContent?.replace(/\s+/g, ' ').trim());
  }

  beforeEach(async () => {
    unreadNotificationCount.set(0);
    await TestBed.configureTestingModule({
      imports: [UserAvatarComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        {
          provide: UserNotificationInboxService,
          useValue: {
            unreadCount: unreadNotificationCount,
            refresh: () => undefined,
            fetchCount: () => EMPTY,
            clear: () => unreadNotificationCount.set(0),
          },
        },
      ],
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

    expect(menuLabels().some(t => t === 'Analytics')).toBe(false);
  });

  it('should show Analytics menu item when analyticsEnabled is true', async () => {
    fixture.componentRef.setInput('analyticsEnabled', true);
    fixture.detectChanges();

    (fixture.nativeElement as HTMLElement).querySelector('.user-avatar')?.dispatchEvent(new MouseEvent('click'));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(menuLabels()).toContain('Analytics');
  });

  it('should show My Workspace after Subscriptions', async () => {
    fixture.componentRef.setInput('analyticsEnabled', false);
    fixture.detectChanges();

    (fixture.nativeElement as HTMLElement).querySelector('.user-avatar')?.dispatchEvent(new MouseEvent('click'));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(menuLabels()).toEqual(['Applications', 'Subscriptions', 'Notifications', 'My Workspace', 'Log out']);
  });

  it('shows the unread notification count in the avatar menu', async () => {
    unreadNotificationCount.set(3);
    fixture.detectChanges();

    (fixture.nativeElement as HTMLElement).querySelector('.user-avatar')?.dispatchEvent(new MouseEvent('click'));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(menuLabels()).toEqual(['Applications', 'Subscriptions', 'Notifications (3)', 'My Workspace', 'Log out']);
  });
});
