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
import { PortalError, ManagementError } from '@model/technical';

export class ErrorAssertions {
  private response: Cypress.Response<PortalError>;
  private error: PortalError;

  private constructor(response: Cypress.Response<PortalError>) {
    this.response = response;
    this.error = response.body;
  }

  static assertThat(response: Cypress.Response<PortalError>) {
    return new ErrorAssertions(response);
  }

  containsMessage(expectedMessage: string) {
    expect(
      this.error.errors.findIndex((e) => e.message.includes(expectedMessage)),
      'contains expected message',
    ).to.be.greaterThan(-1);
    return this;
  }

  containsCode(expectedCode: string) {
    expect(
      this.error.errors.findIndex((e) => e.code.includes(expectedCode)),
      'contains expected code',
    ).to.be.greaterThan(-1);
    return this;
  }
}

export class TechnicalErrorAssertions {
  private response: Cypress.Response<ManagementError>;
  private error: ManagementError;

  private constructor(response: Cypress.Response<ManagementError>) {
    this.response = response;
    this.error = response.body;
  }

  static assertThat(response: Cypress.Response<ManagementError>) {
    return new TechnicalErrorAssertions(response);
  }

  containsMessage(expectedMessage: string) {
    expect(this.error.message, 'contains expected message').to.contains(expectedMessage);
    return this;
  }

  containsCode(expectedCode: string) {
    expect(this.error.technicalCode, 'contains expected code').to.contains(expectedCode);
    return this;
  }
}
