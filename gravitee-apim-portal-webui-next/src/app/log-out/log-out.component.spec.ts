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
import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { LogOutComponent } from './log-out.component';
import { fakeUser } from '../../entities/user/user.fixtures';
import { CurrentUserService } from '../../services/current-user.service';
import { AppTestingModule, TESTING_BASE_URL } from '../../testing/app-testing.module';

describe('LogOutComponent', () => {
  let fixture: ComponentFixture<LogOutComponent>;
  let httpTestingController: HttpTestingController;
  let router: Router;
  let currentUserService: CurrentUserService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LogOutComponent, AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(LogOutComponent);
    httpTestingController = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    currentUserService = TestBed.inject(CurrentUserService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should logout', () => {
    jest.spyOn(router, 'navigate');
    currentUserService.user.set(fakeUser());
    fixture.detectChanges();

    httpTestingController.expectOne(`${TESTING_BASE_URL}/auth/logout`).flush({});
    httpTestingController.expectOne(`${TESTING_BASE_URL}/portal-menu-links`).flush({});
    expect(router.navigate).toHaveBeenCalledTimes(1);
    expect(router.navigate).toHaveBeenCalledWith(['']);
    expect(currentUserService.user()).toEqual({});
  });
});
