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
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpTestingController } from '@angular/common/http/testing';

import { AccountDetailsDialogComponent } from './account-details-dialog.component';
import { CurrentUserService } from '../../services/current-user.service';
import { fakeUser } from '../../entities/user/user.fixtures';
import { AppTestingModule, TESTING_BASE_URL } from '../../testing/app-testing.module';

@Component({
  standalone: true,
  imports: [MatButtonModule, MatDialogModule],
  template: `<button mat-button (click)="open()">Open</button>`,
})
class TestHostComponent {
  constructor(private readonly matDialog: MatDialog) {}

  open() {
    this.matDialog.open(AccountDetailsDialogComponent, {
      data: {
        user: fakeUser({
          id: 'user-1',
          display_name: 'admin',
          first_name: 'Ada',
          last_name: 'Lovelace',
          email: 'ada@example.com',
          editable_profile: true,
          _links: { avatar: `${TESTING_BASE_URL}/user/avatar?1` },
        }),
      },
    });
  }
}

describe('AccountDetailsDialogComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let httpTestingController: HttpTestingController;
  let currentUserService: CurrentUserService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent, AccountDetailsDialogComponent, AppTestingModule, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    currentUserService = TestBed.inject(CurrentUserService);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  async function openDialog(): Promise<void> {
    fixture.nativeElement.querySelector('button')?.click();
    fixture.detectChanges();
    await fixture.whenStable();
  }

  it('should show username, first name, last name, and email', async () => {
    await openDialog();

    expect(document.querySelector('[data-testid="account-details-username"]')?.textContent?.trim()).toBe('admin');
    expect(document.querySelector('[data-testid="account-details-first-name"]')?.textContent?.trim()).toBe('Ada');
    expect(document.querySelector('[data-testid="account-details-last-name"]')?.textContent?.trim()).toBe('Lovelace');
    expect(document.querySelector('[data-testid="account-details-email"]')?.textContent?.trim()).toBe('ada@example.com');
  });

  it('should show avatar image when avatar link is present', async () => {
    await openDialog();
    expect(document.querySelector('[data-testid="account-details-avatar"]')).toBeTruthy();
  });

  it('should save a new avatar via PUT /user', async () => {
    const closeSpy = jest.fn();
    await TestBed.resetTestingModule()
      .configureTestingModule({
        imports: [AccountDetailsDialogComponent, AppTestingModule, NoopAnimationsModule],
        providers: [
          {
            provide: MAT_DIALOG_DATA,
            useValue: {
              user: fakeUser({
                id: 'user-1',
                editable_profile: true,
              }),
            },
          },
          { provide: MatDialogRef, useValue: { close: closeSpy } },
        ],
      })
      .compileComponents();

    const dialogFixture = TestBed.createComponent(AccountDetailsDialogComponent);
    dialogFixture.detectChanges();
    httpTestingController = TestBed.inject(HttpTestingController);
    currentUserService = TestBed.inject(CurrentUserService);

    dialogFixture.componentInstance.pendingAvatarDataUrl.set('data:image/png;base64,abc');
    dialogFixture.componentInstance.saveAvatar();

    const req = httpTestingController.expectOne(`${TESTING_BASE_URL}/user`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ id: 'user-1', avatar: 'data:image/png;base64,abc' });
    const updatedUser = fakeUser({ id: 'user-1', _links: { avatar: `${TESTING_BASE_URL}/user/avatar?2` } });
    req.flush(updatedUser);

    expect(currentUserService.user()).toEqual(updatedUser);
    expect(closeSpy).toHaveBeenCalledWith(true);
  });
});
