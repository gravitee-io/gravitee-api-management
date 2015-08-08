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
package io.gravitee.repositories.mongodb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.repositories.mongodb.internal.model.NodeMongo;
import io.gravitee.repositories.mongodb.internal.node.NodeMongoRepository;
import io.gravitee.repositories.mongodb.mapper.GraviteeMapper;
import io.gravitee.repository.api.NodeRepository;
import io.gravitee.repository.model.Node;

@Component
public class NodeRepositoryImpl implements NodeRepository{

	@Autowired
	private NodeMongoRepository internalNodeRepo;

	@Autowired
	private GraviteeMapper mapper;

	@Override
	public void register(Node node) {
		NodeMongo nodeMongo = mapper.map(node, NodeMongo.class);
		internalNodeRepo.save(nodeMongo);
	}

	@Override
	public void unregister(Node node) {
		internalNodeRepo.delete(node.getName());
	}

}
