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

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.gravitee.repository.api.NodeRepository;
import io.gravitee.repository.model.Node;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { RepositoryConfiguration.class })
public class NodeRepositoryTest extends AbstractMongoDBTest {

	private static final String TESTCASES_PATH = "/data/node-tests/";
	
	private Logger logger = LoggerFactory.getLogger(NodeRepositoryTest.class);	
	
	@Autowired
	private NodeRepository nodeRepository;

    @Override
    protected String getTestCasesPath() {
        return TESTCASES_PATH;
    }

	@Test
	public void register() {
		
		try {
			Node node = new Node();
			node.setCluster("clusterA");
			node.setHost("localhost");
			node.setName("nodeA-1");
			node.setLastStartupTime(new Date());

			nodeRepository.register(node);

		} catch (Exception e) {
			logger.error("Error while testing registering node", e);
			Assert.fail("Error while testing registering node");
		}
	}


	@Test
	public void unregister() {
		try{

			nodeRepository.unregister("node-to-unregister");
			
		}catch(Exception e){
			logger.error("Error while calling findByName", e);
			Assert.fail("Error while calling findByName");		
		}
	}


}
