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

declare global {
  namespace Cypress {
    interface Chainable<Subject> {
      console(method: any): any;
    }
  }
}
export {};

/**
 * Allow to log the previous subject of the command, and pass it to the next one.
 */
Cypress.Commands.add(
  'console',
  {
    prevSubject: true,
  },
  (subject, method) => {
    // the previous subject is automatically received
    // and the commands arguments are shifted

    // allow us to change the console method used
    method = method || 'log';

    // log the subject to the console
    console[method]('The subject is', subject);

    // whatever we return becomes the new subject
    //
    // we don't want to change the subject so
    // we return whatever was passed in
    return subject;
  },
);
