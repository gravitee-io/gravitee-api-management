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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.NodeRepository;
import io.gravitee.repository.management.model.Node;
import io.gravitee.repository.management.model.NodeState;
import io.gravitee.repository.mongodb.management.internal.model.NodeMongo;
import io.gravitee.repository.mongodb.management.internal.node.NodeMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
public class MongoNodeRepository implements NodeRepository {

	@Autowired
	private NodeMongoRepository internalNodeRepo;

	@Autowired
	private GraviteeMapper mapper;

	@Override
	public void register(Node node) throws TechnicalException {
		NodeMongo nodeMongo = mapper.map(node, NodeMongo.class);
		
		nodeMongo.setLastStartupTime(new Date());
		nodeMongo.setState(NodeState.REGISTERED.name());
		
		internalNodeRepo.save(nodeMongo);
	}

	@Override
	public void unregister(String nodename) throws TechnicalException {
		
		NodeMongo node = internalNodeRepo.findOne(nodename);
		
		node.setLastStopTime(new Date());
		node.setState(NodeState.UNREGISTERED.name());
		
		internalNodeRepo.save(node);
	}


	@Override
	public Set<Node> findAll() throws TechnicalException {
		
		List<NodeMongo> nodesMongo = internalNodeRepo.findAll();
		return mapper.collection2set(nodesMongo, NodeMongo.class, Node.class);
	}

}
