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
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.*;
import io.gravitee.repository.redis.management.internal.ApiRedisRepository;
import io.gravitee.repository.redis.management.internal.MembershipRedisRepository;
import io.gravitee.repository.redis.management.model.RedisApi;
import io.gravitee.repository.redis.management.model.RedisMembership;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisApiRepository implements ApiRepository {

    @Autowired
    private ApiRedisRepository apiRedisRepository;

    @Autowired
    private MembershipRedisRepository membershipRedisRepository;

    @Override
    public Set<Api> findAll() throws TechnicalException {
        Set<RedisApi> apis = apiRedisRepository.findAll();

        return apis.stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<Api> findById(String apiId) throws TechnicalException {
        RedisApi redisApi = this.apiRedisRepository.find(apiId);
        return Optional.ofNullable(convert(redisApi));
    }

    @Override
    public Api create(Api api) throws TechnicalException {
        RedisApi redisApi = apiRedisRepository.saveOrUpdate(convert(api));
        return convert(redisApi);
    }

    @Override
    public Api update(Api api) throws TechnicalException {
        RedisApi redisApi = apiRedisRepository.saveOrUpdate(convert(api));
        return convert(redisApi);
    }

    @Override
    public void delete(String apiId) throws TechnicalException {
        apiRedisRepository.delete(apiId);
    }

    @Override
    public Set<Api> findByMember(String username, MembershipType membershipType, Visibility visibility) throws TechnicalException {
        Set<RedisMembership> memberships = membershipRedisRepository.getMemberships(username);

        return memberships.stream()
                .filter(redisMembership ->
                        redisMembership.getMembershipFor() == RedisMembership.MembershipFor.API &&
                                (membershipType == null || (membershipType.name().equals(redisMembership.getMembershipType()
                                ))))
                .map(RedisMembership::getOwner)
                .distinct()
                .map(api -> convert(apiRedisRepository.find(api)))
                .collect(Collectors.toSet());
    }

    @Override
    public void saveMember(String apiId, String username, MembershipType membershipType) throws TechnicalException {
        // Add member into the API
        RedisApi redisApi = apiRedisRepository.find(apiId);
        redisApi.getMembers().add(username);
        apiRedisRepository.saveOrUpdate(redisApi);

        // Save or saveOrUpdate membership entity
        Set<RedisMembership> memberships = membershipRedisRepository.getMemberships(username);
        List<RedisMembership> membershipList = new ArrayList<>(memberships);

        RedisMembership membership = new RedisMembership();
        membership.setOwner(apiId);
        membership.setMembershipFor(RedisMembership.MembershipFor.API);

        int idx = membershipList.indexOf(membership);
        if (idx != -1) {
            membership = membershipList.get(idx);
            membership.setMembershipType(membershipType.name());
            membership.setUpdatedAt(new Date().getTime());
        } else {
            membership.setMembershipType(membershipType.name());
            membership.setCreatedAt(new Date().getTime());
            membership.setUpdatedAt(membership.getCreatedAt());
            memberships.add(membership);
        }
        membershipRedisRepository.save(username, memberships);
    }

    @Override
    public void deleteMember(String apiId, String username) throws TechnicalException {
        // Remove member from the API
        RedisApi redisApi = apiRedisRepository.find(apiId);
        redisApi.getMembers().remove(username);
        apiRedisRepository.saveOrUpdate(redisApi);

        Set<RedisMembership> memberships = membershipRedisRepository.getMemberships(username);

        RedisMembership membership = new RedisMembership();
        membership.setOwner(apiId);
        membership.setMembershipFor(RedisMembership.MembershipFor.API);

        if (memberships.contains(membership)) {
            memberships.remove(membership);
            membershipRedisRepository.save(username, memberships);
        }
    }

    @Override
    public Collection<Membership> getMembers(String apiId, MembershipType membershipType) throws TechnicalException {
        RedisApi api = apiRedisRepository.find(apiId);
        Set<Membership> memberships = new HashSet<>();

        for(String member : api.getMembers()) {
            Set<RedisMembership> redisMemberships = membershipRedisRepository.getMemberships(member);

            redisMemberships.stream()
                    .filter(redisMembership ->
                            (redisMembership.getMembershipFor() == RedisMembership.MembershipFor.API) &&
                                    redisMembership.getOwner().equals(apiId))
                    .filter(membership -> membershipType.name().equalsIgnoreCase(membership.getMembershipType()))
                    .forEach(redisMembership -> memberships.add(convert(redisMembership)));
        }

        return memberships;
    }

    @Override
    public Membership getMember(String apiId, String username) throws TechnicalException {
        Set<RedisMembership> memberships = membershipRedisRepository.getMemberships(username);

        for(RedisMembership redisMembership : memberships) {
            if (redisMembership.getMembershipFor() == RedisMembership.MembershipFor.API &&
                    redisMembership.getOwner().equals(apiId)) {
                return convert(redisMembership);
            }
        }

        return null;
    }

    private Api convert(RedisApi redisApi) {
        if (redisApi == null) {
            return null;
        }

        Api api = new Api();

        api.setId(redisApi.getId());
        api.setName(redisApi.getName());
        api.setCreatedAt(new Date(redisApi.getCreatedAt()));
        api.setUpdatedAt(new Date(redisApi.getUpdatedAt()));
        api.setDeployedAt(new Date(redisApi.getDeployedAt()));
        api.setDefinition(redisApi.getDefinition());
        api.setDescription(redisApi.getDescription());
        api.setVersion(redisApi.getVersion());
        api.setVisibility(Visibility.valueOf(redisApi.getVisibility()));
        api.setLifecycleState(LifecycleState.valueOf(redisApi.getLifecycleState()));

        return api;
    }

    private RedisApi convert(Api api) {
        RedisApi redisApi = new RedisApi();

        redisApi.setId(api.getId());
        redisApi.setName(api.getName());
        redisApi.setCreatedAt(api.getCreatedAt().getTime());
        redisApi.setUpdatedAt(api.getUpdatedAt().getTime());
        redisApi.setDeployedAt(api.getDeployedAt().getTime());
        redisApi.setDefinition(api.getDefinition());
        redisApi.setDescription(api.getDescription());
        redisApi.setVersion(api.getVersion());
        redisApi.setVisibility(api.getVisibility().name());
        redisApi.setLifecycleState(api.getLifecycleState().name());

        return redisApi;
    }

    private Membership convert(RedisMembership redisMembership) {
        Membership membership = new Membership();

        //TODO: map user
        membership.setUser(null);
        membership.setCreatedAt(new Date(redisMembership.getCreatedAt()));
        membership.setUpdatedAt(new Date(redisMembership.getUpdatedAt()));
        membership.setMembershipType(MembershipType.valueOf(redisMembership.getMembershipType()));

        return membership;
    }
}