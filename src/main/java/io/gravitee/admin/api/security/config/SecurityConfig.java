/**
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
package io.gravitee.admin.api.security.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;

import io.gravitee.admin.api.security.adapter.BasicSecurityConfigurerAdapter;
import io.gravitee.admin.api.security.adapter.OAuth2SecurityConfigurerAdapter;

/**
 * 
 * @author Titouan COMPIEGNE
 *
 */
@Configuration
@Import({BasicSecurityConfigurerAdapter.class, OAuth2SecurityConfigurerAdapter.class})
public class SecurityConfig extends GlobalAuthenticationConfigurerAdapter {
	
}
