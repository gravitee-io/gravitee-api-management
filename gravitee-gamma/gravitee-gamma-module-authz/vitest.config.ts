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
import { defineConfig } from 'vitest/config';

export default defineConfig({
    test: {
        environment: 'jsdom',
        globals: true,
        setupFiles: ['./src/test/ui/setup.ts'],
        include: ['src/main/ui/**/__tests__/**/*.test.{ts,tsx}'],
        reporters: ['default', 'junit'],
        outputFile: { junit: './target/coverage/junit-report.xml' },
        coverage: {
            provider: 'v8',
            reportsDirectory: './target/coverage',
            reporter: ['text', 'lcov'],
            include: ['src/main/ui/**/*.{ts,tsx}'],
            exclude: ['src/main/ui/__tests__/**'],
        },
    },
});
