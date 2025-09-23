/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactive.core.context;

import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_EXECUTION_COMPONENT_SCOPE;

import io.gravitee.gateway.reactive.api.ComponentType;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Utility to manage component attribution as a scoped stack.
 */
public final class ComponentScope {

    private ComponentScope() {}

    private static Deque<ComponentEntry> getOrCreateStack(BaseExecutionContext ctx) {
        Deque<ComponentEntry> stack = ctx.getInternalAttribute(ATTR_INTERNAL_EXECUTION_COMPONENT_SCOPE);
        if (stack == null) {
            stack = new ArrayDeque<>();
            ctx.setInternalAttribute(ATTR_INTERNAL_EXECUTION_COMPONENT_SCOPE, stack);
        }
        return stack;
    }

    public static ComponentEntry peek(BaseExecutionContext ctx) {
        Deque<ComponentEntry> stack = ctx.getInternalAttribute(ATTR_INTERNAL_EXECUTION_COMPONENT_SCOPE);
        return stack != null ? stack.peek() : null;
    }

    public static void push(BaseExecutionContext ctx, ComponentType type, String name) {
        getOrCreateStack(ctx).push(new ComponentEntry(type, name));
    }

    /**
     * Remove the specified component from the scope stack.
     * If multiple entries match, the first one from the top of the stack will be removed.
     */
    public static void remove(BaseExecutionContext ctx, ComponentType type, String name) {
        Deque<ComponentEntry> stack = getOrCreateStack(ctx);
        if (stack.isEmpty()) {
            return;
        }
        // Remove the first occurrence starting from the top of the stack
        stack.removeFirstOccurrence(new ComponentEntry(type, name));
    }

    public record ComponentEntry(ComponentType type, String name) {}
}
