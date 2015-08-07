package io.gravitee.repositories.mongodb.mapper;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.dozer.Mapper;

public interface GraviteeMapper extends Mapper {

	 <T,F> Set<T> collection2set(Collection<F> elements, Class<F> formClass, Class<T> toClass);
	 
	 <T,F> List<T> collection2list(Collection<F> elements, Class<F> formClass, Class<T> toClass);
}
