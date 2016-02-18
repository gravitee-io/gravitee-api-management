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
package io.gravitee.repository.redis.junit;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.server.netty.*;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class RedisExternalResource extends ExternalResource {

    private final static Logger logger = LoggerFactory.getLogger(RedisExternalResource.class);

    private final static int REDIS_PORT = 6379;

    private DefaultEventExecutorGroup redisGroup;
    private Channel redisChannel;

    @Override
    protected void before() throws Throwable {
        RedisServer redisServer = new SimpleRedisServer();
        redisServer.flushall();

        final RedisCommandHandler commandHandler = new RedisCommandHandler(redisServer);

        redisGroup = new DefaultEventExecutorGroup(128);

        ServerBootstrap redisServerBootstrap = new ServerBootstrap();

        try {
            redisServerBootstrap.group(new NioEventLoopGroup(), new NioEventLoopGroup())
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .localAddress(REDIS_PORT)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new RedisCommandDecoder());
                            p.addLast(new RedisReplyEncoder());
                            p.addLast(redisGroup, commandHandler);
                        }
                    });

            logger.info("Starting Redis(port={}) server...", REDIS_PORT);
            ChannelFuture future = redisServerBootstrap.bind();
            ChannelFuture syncFuture = future.sync();
            redisChannel = syncFuture.channel();
        } catch (InterruptedException e) {
            logger.error("Unexpected error:", e);
        }
    }

    @Override
    protected void after() {
        logger.info("Shutting down Redis server...");
        ChannelFuture closeFuture = redisChannel.close();
        try {
            closeFuture.sync();

            // The Netty version required by the embedded Redis implementation does not
            // return a Future, so wait until terminated in a loop
                redisGroup.shutdownGracefully();
                while (!redisGroup.isTerminated()) {
                    Thread.sleep(50);
                }
                logger.info("Redis server shutdown completed");
        } catch (InterruptedException e) {
            logger.error("Unexpected error: ", e);
        }
    }
}
