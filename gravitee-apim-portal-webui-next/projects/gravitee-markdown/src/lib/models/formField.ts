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

/**
 * Error codes for form field validation
 */
export type GmdFieldErrorCode = 'required' | 'minLength' | 'maxLength' | 'pattern';

/**
 * State of a form field, including its value, validation status, and errors
 */
export interface GmdFieldState {
  /** Field key used to identify the field in the form */
  key: string;
  /** Current value of the field (always string for metadata) */
  value: string;
  /** Whether the field passes all validation rules */
  valid: boolean;
  /** Whether the field is required */
  required: boolean;
  /** Whether the field has been touched/focused by the user */
  touched: boolean;
  /** Array of validation error codes */
  errors: GmdFieldErrorCode[];
}
