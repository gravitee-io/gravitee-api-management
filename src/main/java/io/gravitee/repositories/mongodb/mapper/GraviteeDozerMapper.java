package io.gravitee.repositories.mongodb.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dozer.DozerBeanMapper;

public class GraviteeDozerMapper extends DozerBeanMapper implements GraviteeMapper {

	public GraviteeDozerMapper(){
		super.addMapping(getClass().getResourceAsStream("/dozer.xml"));
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
