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
import { SearchableUser } from './searchableUser';

export function fakeSearchableUser(attributes?: Partial<SearchableUser>): SearchableUser {
  const base: SearchableUser = {
    id: '1d4fae8c-3705-43ab-8fae-8c370543abf3',
    displayName: 'Bruce Wayne',
    reference:
      'ZXlKamRIa2lPaUpLVjFRaUxDSmxibU1pT2lKQk1qVTJSME5OSWl3aVlXeG5Jam9pWkdseUluMC4uU1hpaXBCSUhaZFpYTTdubC5uQ241WWR1MEhDS3FPOF9uaWpzVHJad2RCaEppVWxVRE9XMnB1dVoya2c0QW9SQi1Vb0o1azdKSndwNXMwSE5kcjU0ZVd0cUN5bUxFWUdOTGJOdlNRZjNOLVFGa3Q4UHhyYmFha05BbEd2NzlUeE5ySWJCYmxrcHhpQnNDRjMwcVY1emJxdjhnZVZ3RjF1RzM0cTZ5R25kRWhjeFAyQ2h0S1lqd3UwcUxES0dqNUkwLlB2NENWZHBPcUM3cnlWcTBFalRwa1E=',
    firstname: 'Bruce',
    lastname: 'Wayne',
  };

  return {
    ...base,
    ...attributes,
  };
}
