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
import { creationButtonLabel, resolveCreationOutcome } from './apiCreationOutcome';

describe('resolveCreationOutcome', () => {
    it('ignores the ask switch while API Review is off', () => {
        expect(resolveCreationOutcome({ askForReview: true, deployImmediately: true }, false)).toEqual({
            askForReview: false,
            deployImmediately: true,
        });
        expect(resolveCreationOutcome({ askForReview: true, deployImmediately: false }, false)).toEqual({
            askForReview: false,
            deployImmediately: false,
        });
    });

    it('never deploys immediately while API Review is on, whatever the ask switch says', () => {
        expect(resolveCreationOutcome({ askForReview: true, deployImmediately: true }, true)).toEqual({
            askForReview: true,
            deployImmediately: false,
        });
        expect(resolveCreationOutcome({ askForReview: false, deployImmediately: true }, true)).toEqual({
            askForReview: false,
            deployImmediately: false,
        });
    });
});

describe('creationButtonLabel', () => {
    it('names the outcome', () => {
        expect(creationButtonLabel({ askForReview: true, deployImmediately: false })).toBe('Create & ask for review');
        expect(creationButtonLabel({ askForReview: false, deployImmediately: true })).toBe('Create & Deploy');
        expect(creationButtonLabel({ askForReview: false, deployImmediately: false })).toBe('Create API');
    });
});
