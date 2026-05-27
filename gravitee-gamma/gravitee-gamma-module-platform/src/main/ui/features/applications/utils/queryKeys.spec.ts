/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { applicationMemberKeys, applicationNotificationKeys } from './queryKeys';

describe('application query keys', () => {
    describe('applicationMemberKeys', () => {
        it('builds stable list and user search keys', () => {
            expect(applicationMemberKeys.list('env', 'app')).toEqual(['application-members', 'list', 'env', 'app']);
            expect(applicationMemberKeys.userSearch('alice')).toEqual(['application-members', 'user-search', 'alice']);
            expect(applicationMemberKeys.userSearchTransfer('bob')).toEqual(['application-members', 'user-search-transfer', 'bob']);
        });
    });

    describe('applicationNotificationKeys', () => {
        it('builds list, notifiers, hooks, and metadata keys', () => {
            expect(applicationNotificationKeys.list('env', 'app')).toEqual(['application-notifications', 'list', 'env', 'app']);
            expect(applicationNotificationKeys.notifiers('env', 'app')).toEqual(['application-notifications', 'notifiers', 'env', 'app']);
            expect(applicationNotificationKeys.hooks('env')).toEqual(['application-notifications', 'hooks', 'env']);
            expect(applicationNotificationKeys.metadata('env', 'app')).toEqual(['application-notifications', 'metadata', 'env', 'app']);
        });
    });
});
