/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import type { CodeSampleLanguage } from '../../ApiSpecBlock/code-sample-generator';
import styles from '../GraviteeDocsRenderer.module.scss';

interface CodeLanguageIconProps {
    readonly language: CodeSampleLanguage;
}

export function CodeLanguageIcon({ language }: CodeLanguageIconProps) {
    switch (language) {
        case 'curl':
            return (
                <svg className={styles.languageIcon} viewBox="0 0 16 16" aria-hidden="true">
                    <rect width="16" height="16" rx="3" fill="#1f2937" />
                    <path
                        d="M4 5.5h8M4 8h5.5M4 10.5h6.5"
                        stroke="#9ca3af"
                        strokeWidth="1.2"
                        strokeLinecap="round"
                    />
                </svg>
            );
        case 'python':
            return (
                <svg className={styles.languageIcon} viewBox="0 0 16 16" aria-hidden="true">
                    <rect width="16" height="16" rx="3" fill="#306998" />
                    <path
                        d="M6.2 4.8h3.1c1.2 0 2.1.9 2.1 2v.8H8.4V6.8H6.2V9.2h3.1v2.1H6.2c-1.2 0-2.1-.9-2.1-2V6.8c0-1.1.9-2 2.1-2Z"
                        fill="#ffd43b"
                    />
                    <circle cx="6.8" cy="5.5" r=".55" fill="#306998" />
                    <circle cx="9.2" cy="10.5" r=".55" fill="#ffd43b" />
                </svg>
            );
        case 'node':
            return (
                <svg className={styles.languageIcon} viewBox="0 0 16 16" aria-hidden="true">
                    <rect width="16" height="16" rx="3" fill="#3c873a" />
                    <path
                        d="M8 3.5 12.2 6v4L8 12.5 3.8 10V6L8 3.5Z"
                        fill="none"
                        stroke="#fff"
                        strokeWidth="1"
                    />
                    <path d="M8 6.2v3.6" stroke="#fff" strokeWidth="1" strokeLinecap="round" />
                </svg>
            );
        case 'javascript':
            return (
                <svg className={styles.languageIcon} viewBox="0 0 16 16" aria-hidden="true">
                    <rect width="16" height="16" rx="3" fill="#f7df1e" />
                    <path
                        d="M6.2 11.2c.3.5.8.9 1.7.9 1.1 0 1.5-.6 1.5-1.4V7.2H9.4v3.3c0 .3-.2.6-.7.6-.5 0-.7-.3-.9-.6L6.2 11.2Zm4.8-3.9c.3-.5.8-.9 1.6-.9 1 0 1.3.5 1.3 1.1 0 1.1-1.4 1.4-1.4 2.1 0 .2.2.4.5.4.4 0 .7-.2 1-.6l.9.8c-.5.7-1.2 1.1-2 1.1-1.1 0-1.8-.7-1.8-1.7 0-1.2 1.4-1.5 1.4-2.3 0-.3-.3-.4-.6-.4-.5 0-.8.3-1 .6l-.9-.8Z"
                        fill="#000"
                    />
                </svg>
            );
        case 'java':
            return (
                <svg className={styles.languageIcon} viewBox="0 0 16 16" aria-hidden="true">
                    <rect width="16" height="16" rx="3" fill="#5382a1" />
                    <path
                        d="M6.2 5.8c1.6-.9 3.6-.8 3.6-.8s-.5 1.2-2.1 1.7c-1.2.4-2 .1-1.5-.9Zm-.2 2.4c1.8 1.1 4.2.7 4.2.7s-.7 1-2.7 1.1c-1.6.1-2.4-.4-1.5-1.8Zm1.1 2.2c1.5 1.4 4.4 1 4.4 1s-.5 1.3-3.3 1.3c-2 .1-2.7-.6-1.1-2.3Z"
                        fill="#f8981d"
                    />
                </svg>
            );
        case 'go':
            return (
                <svg className={styles.languageIcon} viewBox="0 0 16 16" aria-hidden="true">
                    <rect width="16" height="16" rx="3" fill="#00add8" />
                    <path
                        d="M4.2 6.2h1.1l.5 2.4.5-2.4h1l-.8 3.6H5l-.8-3.6Zm4.1 0h2.2c.8 0 1.3.4 1.3 1.1 0 .5-.2.8-.6 1l.7 1.5H10L9.4 8.5h-.6v1.3H7.7V6.2Zm.6 1.7h.6c.3 0 .5-.1.5-.4s-.2-.4-.5-.4h-.6v.8Z"
                        fill="#fff"
                    />
                </svg>
            );
        default:
            return null;
    }
}
