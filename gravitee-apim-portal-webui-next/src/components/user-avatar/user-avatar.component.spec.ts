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
import { MatDialog } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import { UserAvatarComponent } from './user-avatar.component';
import { fakeUser } from '../../entities/user/user.fixtures';
import { ConfigService } from '../../services/config.service';
import { ConfigServiceStub } from '../../testing/app-testing.module';
import { AccountDetailsDialogComponent } from '../account-details-dialog/account-details-dialog.component';

describe('UserAvatarComponent', () => {
  let fixture: ComponentFixture<UserAvatarComponent>;
  let matDialogOpen: jest.SpyInstance;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserAvatarComponent, NoopAnimationsModule],
      providers: [provideRouter([]), { provide: ConfigService, useClass: ConfigServiceStub }],
    }).compileComponents();

    fixture = TestBed.createComponent(UserAvatarComponent);
    fixture.componentRef.setInput('user', fakeUser());
    fixture.detectChanges();
    matDialogOpen = jest.spyOn(TestBed.inject(MatDialog), 'open').mockReturnValue({} as never);
  });

  async function openMenu(): Promise<string[]> {
    (fixture.nativeElement as HTMLElement).querySelector('.user-avatar')?.dispatchEvent(new MouseEvent('click'));
    fixture.detectChanges();
    await fixture.whenStable();
    const panel = document.querySelector('.mat-mdc-menu-panel');
    return Array.from(panel?.querySelectorAll('.mat-mdc-menu-item') ?? []).map(el => el.textContent?.trim() ?? '');
  }

  it('should show My account menu item', async () => {
    const labels = await openMenu();
    expect(labels).toContain('My account');
  });

  it('should open account details dialog from My account', async () => {
    await openMenu();
    const myAccount = Array.from(document.querySelectorAll('.mat-mdc-menu-item')).find(el => el.textContent?.trim() === 'My account') as
      | HTMLElement
      | undefined;
    myAccount?.click();
    fixture.detectChanges();

    expect(matDialogOpen).toHaveBeenCalledWith(
      AccountDetailsDialogComponent,
      expect.objectContaining({
        data: { user: fakeUser() },
        width: '420px',
      }),
    );
  });

  it('should not show Analytics menu item when analyticsEnabled is false', async () => {
    fixture.componentRef.setInput('analyticsEnabled', false);
    fixture.detectChanges();

    const labels = await openMenu();
    expect(labels.some(t => t === 'Analytics')).toBe(false);
  });

  it('should show Analytics menu item when analyticsEnabled is true', async () => {
    fixture.componentRef.setInput('analyticsEnabled', true);
    fixture.detectChanges();

    const labels = await openMenu();
    expect(labels).toContain('Analytics');
  });
});
