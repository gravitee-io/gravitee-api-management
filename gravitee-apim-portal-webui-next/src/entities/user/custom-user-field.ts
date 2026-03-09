/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
 * Definition of addition user registration fields
 */
export interface CustomUserField {
  /**
   * The field identifier.
   */
  key?: string;
  /**
   * The default field label.
   */
  label?: string;
  /**
   * The field is mandatory
   */
  required?: boolean;
  /**
   * List of authorized values for the field
   */
  values?: Array<string>;
}
