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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { By } from '@angular/platform-browser';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';

import { SearchUsersDialogComponent, SearchUsersDialogData, SearchUsersDialogResult } from './search-users-dialog.component';
import { SearchUsersDialogHarness } from './search-users-dialog.harness';
import { fakeApplicationRoles, fakeSearchUsersResponse } from '../../../../../entities/application-members/application-members.fixture';
import { ConfigService } from '../../../../../services/config.service';
import { TESTING_BASE_URL } from '../../../../../testing/app-testing.module';

const FAKE_ROLES = fakeApplicationRoles().filter(r => !r.system);

const DIALOG_DATA: SearchUsersDialogData = {
  applicationId: 'app-123',
  roles: FAKE_ROLES,
};

@Component({
  selector: 'app-test-host',
  template: `<button (click)="openDialog()">Open</button>`,
  standalone: true,
})
class TestHostComponent {
  result: SearchUsersDialogResult | null = null;

  constructor(private readonly matDialog: MatDialog) {}

  openDialog(): void {
    this.matDialog
      .open<SearchUsersDialogComponent, SearchUsersDialogData, SearchUsersDialogResult | null>(SearchUsersDialogComponent, {
        id: 'searchUsersDialog',
        data: DIALOG_DATA,
      })
      .afterClosed()
      .subscribe(r => (this.result = r ?? null));
  }
}

describe('SearchUsersDialogComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let rootHarnessLoader: HarnessLoader;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent, SearchUsersDialogComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ConfigService, useValue: { baseURL: TESTING_BASE_URL } },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: { isFocusable: () => true, isTabbable: () => true },
      })
      .compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    http = TestBed.inject(HttpTestingController);
    rootHarnessLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);
    fixture.detectChanges();

    const btn: HTMLButtonElement = fixture.debugElement.query(By.css('button')).nativeElement;
    btn.click();
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  async function getDialog(): Promise<SearchUsersDialogHarness> {
    return rootHarnessLoader.getHarness(SearchUsersDialogHarness);
  }

  it('should pre-select the default role', async () => {
    const dialog = await getDialog();
    expect(await dialog.getSelectedRole()).toBe('VIEWER');
  });

  it('should disable Add button when no users selected', async () => {
    const dialog = await getDialog();
    expect(await dialog.isAddDisabled()).toBe(true);
  });

  it('should return null on cancel', async () => {
    const dialog = await getDialog();
    await dialog.cancel();

    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.componentInstance.result).toBeNull();
  });

  it('should have search input rendered', async () => {
    const document = fixture.nativeElement.ownerDocument as Document;
    const input = document.querySelector('[data-testid="user-search-input"]');
    expect(input).toBeTruthy();
  });
});
