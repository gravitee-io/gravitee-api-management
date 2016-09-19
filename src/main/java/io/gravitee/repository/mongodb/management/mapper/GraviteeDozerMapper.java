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
package io.gravitee.repository.mongodb.management.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dozer.DozerBeanMapper;
import org.dozer.MappingException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeDozerMapper extends DozerBeanMapper implements GraviteeMapper {

	public GraviteeDozerMapper(){
		super.addMapping(getClass().getResourceAsStream("/dozer.xml"));
	}
	
	public  <T> T map(Object source, Class<T> destinationClass) throws MappingException{
		if(source == null) 
			return null;
		return super.map(source, destinationClass);
	}
	  
	public <T,F> Set<T> collection2set(Collection<F> elements, Class<F> formClass, Class<T> toClass){
		
		Set<T> res = new HashSet<>();
		for (F elt : elements) {
			res.add(map(elt,toClass));
		}
		return res;
	}
	
	public  <T,F> List<T> collection2list(Collection<F> elements, Class<F> formClass, Class<T> toClass){
		
		List<T> res = new ArrayList<>();
		for (F elt : elements) {
			res.add(map(elt,toClass));
		}
		return res;
	}
}
