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

import com.github.dozermapper.core.DozerBeanMapperBuilder;
import com.github.dozermapper.core.Mapper;
import com.github.dozermapper.core.MapperModelContext;
import com.github.dozermapper.core.MappingException;
import java.util.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeDozerMapper implements GraviteeMapper {

    private final Mapper mapper;

    public GraviteeDozerMapper() {
        mapper = DozerBeanMapperBuilder.create().withMappingFiles("dozer.xml").withClassLoader(new RepositoryDozerClassLoader()).build();
    }

    public <T> T map(Object source, Class<T> destinationClass) throws MappingException {
        if (source == null) return null;
        return mapper.map(source, destinationClass);
    }

    @Override
    public void map(Object o, Object o1) throws MappingException {
        mapper.map(o, o1);
    }

    @Override
    public <T> T map(Object o, Class<T> aClass, String s) throws MappingException {
        return mapper.map(o, aClass, s);
    }

    @Override
    public void map(Object o, Object o1, String s) throws MappingException {
        mapper.map(o, o1, s);
    }

    @Override
    public MapperModelContext getMapperModelContext() {
        return mapper.getMapperModelContext();
    }

    public <T, F> Set<T> collection2set(Collection<F> elements, Class<F> formClass, Class<T> toClass) {
        Set<T> res = new HashSet<>();
        for (F elt : elements) {
            res.add(map(elt, toClass));
        }
        return res;
    }

    public <T, F> List<T> collection2list(Collection<F> elements, Class<F> formClass, Class<T> toClass) {
        List<T> res = new ArrayList<>();
        for (F elt : elements) {
            res.add(map(elt, toClass));
        }
        return res;
    }
}
