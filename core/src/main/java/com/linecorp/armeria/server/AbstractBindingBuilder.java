/*
 * Copyright 2019 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.linecorp.armeria.server;

import static com.google.common.base.Preconditions.checkArgument;
import static com.linecorp.armeria.common.HttpMethod.CONNECT;
import static com.linecorp.armeria.common.HttpMethod.DELETE;
import static com.linecorp.armeria.common.HttpMethod.GET;
import static com.linecorp.armeria.common.HttpMethod.HEAD;
import static com.linecorp.armeria.common.HttpMethod.OPTIONS;
import static com.linecorp.armeria.common.HttpMethod.PATCH;
import static com.linecorp.armeria.common.HttpMethod.POST;
import static com.linecorp.armeria.common.HttpMethod.PUT;
import static com.linecorp.armeria.common.HttpMethod.TRACE;
import static com.linecorp.armeria.server.HttpHeaderUtil.ensureUniqueMediaTypes;
import static java.util.Objects.requireNonNull;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.MediaType;

/**
 * An abstract builder class for binding something to a {@link Route} fluently.
 */
abstract class AbstractBindingBuilder {

    private Set<HttpMethod> methods = ImmutableSet.of();
    private Set<MediaType> consumeTypes = ImmutableSet.of();
    private Set<MediaType> produceTypes = ImmutableSet.of();
    private final Map<RouteBuilder, Set<HttpMethod>> routeBuilders = new LinkedHashMap<>();
    private final Set<RouteBuilder> pathBuilders = new LinkedHashSet<>();

    /**
     * Sets the path pattern that a {@link Service} will be bound to.
     * Please refer to the <a href="https://line.github.io/armeria/server-basics.html#path-patterns">Path patterns</a>
     * in order to learn how to specify a path pattern.
     *
     * @throws IllegalArgumentException if the specified path pattern is invalid
     */
    public AbstractBindingBuilder path(String pathPattern) {
        pathBuilders.add(Route.builder().path(requireNonNull(pathPattern, "pathPattern")));
        return this;
    }

    /**
     * Sets the specified prefix which is a directory that a {@link Service} will be bound under.
     * {@code pathUnder("/my/path")} is identical to {@code path("prefix:/my/path")}.
     *
     * @throws IllegalArgumentException if the specified path pattern is invalid
     * @deprecated Use {@link #pathPrefix(String)}.
     */
    @Deprecated
    public AbstractBindingBuilder pathUnder(String prefix) {
        return pathPrefix(prefix);
    }

    /**
     * Sets the specified prefix which is a directory that a {@link Service} will be bound under.
     * {@code pathPrefix("/my/path")} is identical to {@code path("prefix:/my/path")}.
     *
     * @throws IllegalArgumentException if the specified path pattern is invalid
     */
    public AbstractBindingBuilder pathPrefix(String prefix) {
        pathBuilders.add(Route.builder().pathPrefix(requireNonNull(prefix, "prefix")));
        return this;
    }

    /**
     * Sets the path pattern that a {@link Service} will be bound to, only supporting {@link HttpMethod#GET}
     * requests.
     * Please refer to the <a href="https://line.github.io/armeria/server-basics.html#path-patterns">Path patterns</a>
     * in order to learn how to specify a path pattern.
     *
     * @throws IllegalArgumentException if the specified path pattern is invalid
     */
    public AbstractBindingBuilder get(String pathPattern) {
        addRouteBuilder(pathPattern, GET);
        return this;
    }

    /**
     * Sets the path pattern that a {@link Service} will be bound to, only supporting {@link HttpMethod#POST}
     * requests.
     * Please refer to the <a href="https://line.github.io/armeria/server-basics.html#path-patterns">Path patterns</a>
     * in order to learn how to specify a path pattern.
     *
     * @throws IllegalArgumentException if the specified path pattern is invalid
     */
    public AbstractBindingBuilder post(String pathPattern) {
        addRouteBuilder(pathPattern, POST);
        return this;
    }

    /**
     * Sets the path pattern that a {@link Service} will be bound to, only supporting {@link HttpMethod#PUT}
     * requests.
     * Please refer to the <a href="https://line.github.io/armeria/server-basics.html#path-patterns">Path patterns</a>
     * in order to learn how to specify a path pattern.
     *
     * @throws IllegalArgumentException if the specified path pattern is invalid
     */
    public AbstractBindingBuilder put(String pathPattern) {
        addRouteBuilder(pathPattern, PUT);
        return this;
    }

    /**
     * Sets the path pattern that a {@link Service} will be bound to, only supporting {@link HttpMethod#PATCH}
     * requests.
     * Please refer to the <a href="https://line.github.io/armeria/server-basics.html#path-patterns">Path patterns</a>
     * in order to learn how to specify a path pattern.
     *
     * @throws IllegalArgumentException if the specified path pattern is invalid
     */
    public AbstractBindingBuilder patch(String pathPattern) {
        addRouteBuilder(pathPattern, PATCH);
        return this;
    }

    /**
     * Sets the path pattern that a {@link Service} will be bound to, only supporting {@link HttpMethod#DELETE}
     * requests.
     * Please refer to the <a href="https://line.github.io/armeria/server-basics.html#path-patterns">Path patterns</a>
     * in order to learn how to specify a path pattern.
     *
     * @throws IllegalArgumentException if the specified path pattern is invalid
     */
    public AbstractBindingBuilder delete(String pathPattern) {
        addRouteBuilder(pathPattern, DELETE);
        return this;
    }

    /**
     * Sets the path pattern that a {@link Service} will be bound to, only supporting {@link HttpMethod#OPTIONS}
     * requests.
     * Please refer to the <a href="https://line.github.io/armeria/server-basics.html#path-patterns">Path patterns</a>
     * in order to learn how to specify a path pattern.
     *
     * @throws IllegalArgumentException if the specified path pattern is invalid
     */
    public AbstractBindingBuilder options(String pathPattern) {
        addRouteBuilder(pathPattern, OPTIONS);
        return this;
    }

    /**
     * Sets the path pattern that a {@link Service} will be bound to, only supporting {@link HttpMethod#HEAD}
     * requests.
     * Please refer to the <a href="https://line.github.io/armeria/server-basics.html#path-patterns">Path patterns</a>
     * in order to learn how to specify a path pattern.
     *
     * @throws IllegalArgumentException if the specified path pattern is invalid
     */
    public AbstractBindingBuilder head(String pathPattern) {
        addRouteBuilder(pathPattern, HEAD);
        return this;
    }

    /**
     * Sets the path pattern that a {@link Service} will be bound to, only supporting {@link HttpMethod#TRACE}
     * requests.
     * Please refer to the <a href="https://line.github.io/armeria/server-basics.html#path-patterns">Path patterns</a>
     * in order to learn how to specify a path pattern.
     *
     * @throws IllegalArgumentException if the specified path pattern is invalid
     */
    public AbstractBindingBuilder trace(String pathPattern) {
        addRouteBuilder(pathPattern, TRACE);
        return this;
    }

    /**
     * Sets the path pattern that a {@link Service} will be bound to, only supporting {@link HttpMethod#CONNECT}
     * requests.
     * Please refer to the <a href="https://line.github.io/armeria/server-basics.html#path-patterns">Path patterns</a>
     * in order to learn how to specify a path pattern.
     *
     * @throws IllegalArgumentException if the specified path pattern is invalid
     */
    public AbstractBindingBuilder connect(String pathPattern) {
        addRouteBuilder(pathPattern, CONNECT);
        return this;
    }

    private void addRouteBuilder(String pathPattern, HttpMethod method) {
        addRouteBuilder(Route.builder().path(requireNonNull(pathPattern, "pathPattern")), EnumSet.of(method));
    }

    private void addRouteBuilder(RouteBuilder routeBuilder, Set<HttpMethod> methods) {
        final Set<HttpMethod> methodSet = routeBuilders.computeIfAbsent(
                routeBuilder, key -> EnumSet.noneOf(HttpMethod.class));

        for (HttpMethod method : methods) {
            if (!methodSet.add(method)) {
                throw new IllegalArgumentException("duplicate HTTP method: " + method +
                                                   ", for: " + routeBuilder);
            }
        }
    }

    /**
     * Sets the {@link HttpMethod}s that a {@link Service} will support. If not set,
     * {@link HttpMethod#knownMethods()}s are set.
     *
     * @see #path(String)
     * @see #pathPrefix(String)
     */
    public AbstractBindingBuilder methods(HttpMethod... methods) {
        return methods(ImmutableSet.copyOf(requireNonNull(methods, "methods")));
    }

    /**
     * Sets the {@link HttpMethod}s that a {@link Service} will support. If not set,
     * {@link HttpMethod#knownMethods()}s are set.
     *
     * @see #path(String)
     * @see #pathPrefix(String)
     */
    public AbstractBindingBuilder methods(Iterable<HttpMethod> methods) {
        requireNonNull(methods, "methods");
        checkArgument(!Iterables.isEmpty(methods), "methods can't be empty");
        this.methods = Sets.immutableEnumSet(methods);
        return this;
    }

    /**
     * Sets {@link MediaType}s that a {@link Service} will consume. If not set, the {@link Service}
     * will accept all media types.
     */
    public AbstractBindingBuilder consumes(MediaType... consumeTypes) {
        consumes(ImmutableSet.copyOf(requireNonNull(consumeTypes, "consumeTypes")));
        return this;
    }

    /**
     * Sets {@link MediaType}s that a {@link Service} will consume. If not set, the {@link Service}
     * will accept all media types.
     */
    public AbstractBindingBuilder consumes(Iterable<MediaType> consumeTypes) {
        ensureUniqueMediaTypes(consumeTypes, "consumeTypes");
        this.consumeTypes = ImmutableSet.copyOf(consumeTypes);
        return this;
    }

    /**
     * Sets {@link MediaType}s that a {@link Service} will produce to be used in
     * content negotiation. See <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">Accept header</a>
     * for more information.
     */
    public AbstractBindingBuilder produces(MediaType... produceTypes) {
        produces(ImmutableSet.copyOf(requireNonNull(produceTypes, "produceTypes")));
        return this;
    }

    /**
     * Sets {@link MediaType}s that a {@link Service} will produce to be used in
     * content negotiation. See <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">Accept header</a>
     * for more information.
     */
    public AbstractBindingBuilder produces(Iterable<MediaType> produceTypes) {
        ensureUniqueMediaTypes(produceTypes, "produceTypes");
        this.produceTypes = ImmutableSet.copyOf(produceTypes);
        return this;
    }

    /**
     * Returns a newly-created {@link Route}s based on the properties of this builder.
     */
    final List<Route> buildRouteList() {
        final Builder<Route> builder = ImmutableList.builder();

        if (pathBuilders.isEmpty() && routeBuilders.isEmpty()) {
            throw new IllegalStateException(
                    "Should set at least one path that the service is bound to before calling this.");
        }
        if (pathBuilders.isEmpty() && !methods.isEmpty()) {
            throw new IllegalStateException("Should set a path when the methods are set: " + methods);
        }

        if (!pathBuilders.isEmpty()) {
            final Set<HttpMethod> pathMethods = methods.isEmpty() ? HttpMethod.knownMethods() : methods;
            pathBuilders.forEach(pathBuilder -> addRouteBuilder(pathBuilder, pathMethods));
        }
        routeBuilders.forEach((routeBuilder, routeMethods) -> {
            builder.add(routeBuilder.methods(routeMethods)
                                    .consumes(consumeTypes)
                                    .produces(produceTypes)
                                    .build());
        });

        return builder.build();
    }
}

