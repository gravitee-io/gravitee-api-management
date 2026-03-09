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
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ConfigService } from './config.service';
import { CustomUserField } from '../entities/user/custom-user-field';
import { User } from '../entities/user/user';

export interface RegisterUserInput {
  /**
   * Valid email of the new user.
   */
  email: string;
  /**
   * First name of the new user.
   */
  firstname?: string;
  /**
   * Last name of the new user.
   */
  lastname?: string;
  /**
   * URL of the confirmation page to be used in the \'User Registration\' email.
   */
  confirmation_page_url?: string;
  /**
   * Values for CustomUserFields
   */
  customFields?: { [key: string]: string };
}

export interface FinalizeRegistrationInput {
  /**
   * Token of the registered user to be validated.
   */
  token: string;
  /**
   * Password of the registered user.
   */
  password: string;
  /**
   * First name of the registered user.
   */
  firstname: string;
  /**
   * Last name of the registered user.
   */
  lastname: string;
}

@Injectable({
  providedIn: 'root',
})
export class UsersService {
  constructor(
    private readonly http: HttpClient,
    private readonly configService: ConfigService,
  ) {}

  listCustomUserFields(): Observable<Array<CustomUserField>> {
    return this.http.get<Array<CustomUserField>>(`${this.configService.baseURL}/configuration/users/custom-fields`);
  }

  registerNewUser(user: RegisterUserInput) {
    return this.http.post<User>(`${this.configService.baseURL}/users/registration`, user);
  }

  finalizeRegistration(finalizeInput: FinalizeRegistrationInput) {
    return this.http.post<User>(`${this.configService.baseURL}/users/registration/_finalize`, finalizeInput);
  }
}
