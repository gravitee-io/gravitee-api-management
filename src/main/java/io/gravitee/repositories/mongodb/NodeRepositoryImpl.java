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
