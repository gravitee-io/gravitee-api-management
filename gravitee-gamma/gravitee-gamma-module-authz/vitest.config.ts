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
