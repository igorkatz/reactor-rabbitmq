/*
 * Copyright (c) 2018-2019 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.rabbitmq;

import com.rabbitmq.client.Connection;
import reactor.core.publisher.Mono;

public class ChannelPoolFactory {

    public static ChannelPool createChannelPool(Mono<? extends Connection> connectionMono) {
        return createChannelPool(connectionMono, new ChannelPoolOptions());
    }

    public static ChannelPool createChannelPool(Mono<? extends Connection> connectionMono, ChannelPoolOptions channelPoolOptions) {
        return new LazyChannelPool(connectionMono, channelPoolOptions);
    }

}
