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

import com.linecorp.armeria.common.RpcRequest;
import com.linecorp.armeria.common.RpcResponse;

/**
 * An {@link RpcService} that decorates another {@link RpcService}.
 *
 * @see Service#decorate(DecoratingServiceFunction)
 */
public abstract class SimpleDecoratingRpcService extends SimpleDecoratingService<RpcRequest, RpcResponse>
        implements RpcService {
    /**
     * Creates a new instance that decorates the specified {@link Service}.
     */
    protected SimpleDecoratingRpcService(Service<RpcRequest, RpcResponse> delegate) {
        super(delegate);
    }
}
