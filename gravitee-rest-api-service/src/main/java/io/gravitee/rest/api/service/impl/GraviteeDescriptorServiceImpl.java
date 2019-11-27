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
package io.gravitee.rest.api.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.gravitee.rest.api.model.descriptor.GraviteeDescriptorEntity;
import io.gravitee.rest.api.service.GraviteeDescriptorService;
import io.gravitee.rest.api.service.exceptions.GraviteeDescriptorReadException;
import io.gravitee.rest.api.service.exceptions.GraviteeDescriptorVersionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class GraviteeDescriptorServiceImpl extends TransactionalService implements GraviteeDescriptorService {

	private static final String DESCRIPTOR_FILENAME = ".gravitee.json";

	private static final Logger logger = LoggerFactory.getLogger(GraviteeDescriptorServiceImpl.class);

	@Override
	public String descriptorName() {
		return DESCRIPTOR_FILENAME;
	}

	@Override
	public GraviteeDescriptorEntity read(String s) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			GraviteeDescriptorEntity descriptorEntity = mapper.readValue(s, GraviteeDescriptorEntity.class);
			assertVersion(descriptorEntity);
			return descriptorEntity;
		} catch (IOException e) {
			logger.error("An error occurs while trying to read the descriptor", e);
			throw new GraviteeDescriptorReadException("An error occurs while trying to read the descriptor");
		}
	}

	private void assertVersion(GraviteeDescriptorEntity descriptorEntity) {
		if (1 != descriptorEntity.getVersion()) {
			throw new GraviteeDescriptorVersionException(descriptorEntity.getVersion());
		}
	}
}
