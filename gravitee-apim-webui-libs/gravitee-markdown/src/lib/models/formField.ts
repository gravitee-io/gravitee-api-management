/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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

/**
 * Error codes for form field validation (user-facing validation)
 */
export type GmdFieldErrorCode = 'required' | 'minLength' | 'maxLength' | 'pattern';

/**
 * Configuration error codes (admin-facing validation)
 */
export type GmdConfigErrorCode =
  | 'invalidRegex' // Pattern string is not a valid RegExp
  | 'emptyFieldKey' // fieldKey is empty or whitespace
  | 'duplicateKey' // Multiple fields share the same fieldKey (detected in the editor)
  | 'normalizedValue'; // Value was auto-adjusted (warning)

/**
 * Configuration error with context and severity
 */
export interface GmdConfigError {
  /** Error/warning code */
  code: GmdConfigErrorCode;
  /** Human-readable message */
  message: string;
  /** Severity level */
  severity: 'error' | 'warning';
  /** Which property caused the error (e.g., 'pattern', 'fieldKey') */
  field?: string;
  /** The invalid/original value */
  value?: string;
  /** For warnings: what the value was normalized to */
  normalizedTo?: string;
}

/**
 * State of a form field, including its value, validation status, and errors
 */
export interface GmdFieldState {
  /** Unique component instance ID (always unique) */
  id: string;
  /** Field key used to identify the field in the form */
  fieldKey: string;
  /** Current value of the field (always string for metadata) */
  value: string;
  /** Whether the field passes all validation rules */
  valid: boolean;
  /** Whether the field is required */
  required: boolean;
  /** Whether the field has been touched/focused by the user */
  touched: boolean;
  /** Array of user validation error codes */
  validationErrors: GmdFieldErrorCode[];
  /** Array of configuration errors/warnings (admin-facing) */
  configErrors?: GmdConfigError[];
}
