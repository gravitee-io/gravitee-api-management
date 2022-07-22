package io.gravitee.repository.mongodb.management.mapper.factory;/**
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

import com.github.dozermapper.core.BeanFactory;
import com.github.dozermapper.core.config.BeanContainer;
import io.gravitee.repository.management.model.flow.selector.FlowChannelSelector;
import io.gravitee.repository.management.model.flow.selector.FlowConditionSelector;
import io.gravitee.repository.management.model.flow.selector.FlowHttpSelector;
import java.util.HashSet;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CustomFlowSelectorFactory implements BeanFactory {

    public Object createBean(Object source, Class<?> sourceClass, String targetBeanId, BeanContainer beanContainer) {
        if (source instanceof FlowHttpSelector) {
            FlowHttpSelector sourceFlowHttpSelector = (FlowHttpSelector) source;
            FlowHttpSelector flowHttpSelector = new FlowHttpSelector();
            flowHttpSelector.setMethods(new HashSet<>(sourceFlowHttpSelector.getMethods()));
            flowHttpSelector.setPathOperator(sourceFlowHttpSelector.getPathOperator());
            flowHttpSelector.setPath(sourceFlowHttpSelector.getPath());
            return flowHttpSelector;
        } else if (source instanceof FlowConditionSelector) {
            FlowConditionSelector sourceFlowConditionSelector = (FlowConditionSelector) source;
            FlowConditionSelector flowConditionSelector = new FlowConditionSelector();
            flowConditionSelector.setCondition(sourceFlowConditionSelector.getCondition());
            return flowConditionSelector;
        } else if (source instanceof FlowChannelSelector) {
            FlowChannelSelector sourceFlowChannelSelector = (FlowChannelSelector) source;
            FlowChannelSelector flowChannelSelector = new FlowChannelSelector();
            flowChannelSelector.setChannel(sourceFlowChannelSelector.getChannel());
            flowChannelSelector.setChannelOperator(sourceFlowChannelSelector.getChannelOperator());
            flowChannelSelector.setOperations(new HashSet<>(sourceFlowChannelSelector.getOperations()));
            return flowChannelSelector;
        }
        return source;
    }
}
