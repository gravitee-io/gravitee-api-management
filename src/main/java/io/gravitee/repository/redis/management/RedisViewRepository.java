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
package io.gravitee.repository.redis.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ViewRepository;
import io.gravitee.repository.management.model.View;
import io.gravitee.repository.redis.management.internal.ViewRedisRepository;
import io.gravitee.repository.redis.management.model.RedisView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisViewRepository implements ViewRepository {

    @Autowired
    private ViewRedisRepository viewRedisRepository;

    @Override
    public Optional<View> findById(final String viewId) throws TechnicalException {
        final RedisView redisView = viewRedisRepository.findById(viewId);
        return Optional.ofNullable(convert(redisView));
    }

    @Override
    public View create(final View view) throws TechnicalException {
        final RedisView redisView = viewRedisRepository.saveOrUpdate(convert(view));
        return convert(redisView);
    }

    @Override
    public View update(final View view) throws TechnicalException {
        if (view == null || view.getName() == null) {
            throw new IllegalStateException("View to update must have a name");
        }

        final RedisView viewRedis = viewRedisRepository.findById(view.getId());

        if (viewRedis == null) {
            throw new IllegalStateException(String.format("No view found with name [%s]", view.getId()));
        }
        
        final RedisView redisView = viewRedisRepository.saveOrUpdate(convert(view));
        return convert(redisView);
    }

    @Override
    public Set<View> findAll() throws TechnicalException {
        final Set<RedisView> views = viewRedisRepository.findAll();

        return views.stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public void delete(final String viewId) throws TechnicalException {
        viewRedisRepository.delete(viewId);
    }

    private View convert(final RedisView redisView) {
        if (redisView == null) {
            return null;
        }
        final View view = new View();
        view.setId(redisView.getId());
        view.setName(redisView.getName());
        view.setDescription(redisView.getDescription());
        view.setDefaultView(redisView.isDefaultView());
        view.setHidden(redisView.isHidden());
        view.setOrder(redisView.getOrder());
        if (redisView.getCreatedAt() > 0) {
            view.setCreatedAt(new Date(redisView.getCreatedAt()));
        }
        if (redisView.getUpdatedAt() > 0) {
            view.setUpdatedAt(new Date(redisView.getUpdatedAt()));
        }
        return view;
    }

    private RedisView convert(final View view) {
        if (view == null) {
            return null;
        }
        final RedisView redisView = new RedisView();
        redisView.setId(view.getId());
        redisView.setName(view.getName());
        redisView.setDescription(view.getDescription());
        redisView.setDefaultView(view.isDefaultView());
        redisView.setHidden(view.isHidden());
        redisView.setOrder(view.getOrder());
        if (view.getCreatedAt() != null) {
            redisView.setCreatedAt(view.getCreatedAt().getTime());
        }
        if (view.getUpdatedAt() != null) {
            redisView.setUpdatedAt(view.getUpdatedAt().getTime());
        }
        return redisView;
    }
}
