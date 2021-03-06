/*
 * Copyright 2016-present Open Networking Foundation
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
package io.atomix.core.set.impl;

import com.google.common.io.BaseEncoding;
import io.atomix.core.set.AsyncDistributedSet;
import io.atomix.core.set.DistributedSet;
import io.atomix.core.set.DistributedSetBuilder;
import io.atomix.core.set.DistributedSetConfig;
import io.atomix.primitive.PrimitiveManagementService;
import io.atomix.primitive.proxy.ProxyClient;
import io.atomix.primitive.service.ServiceConfig;
import io.atomix.utils.serializer.Serializer;

import java.util.concurrent.CompletableFuture;

/**
 * Default distributed set builder.
 *
 * @param <E> type for set elements
 */
public class DefaultDistributedSetBuilder<E> extends DistributedSetBuilder<E> {
  public DefaultDistributedSetBuilder(String name, DistributedSetConfig config, PrimitiveManagementService managementService) {
    super(name, config, managementService);
  }

  @Override
  @SuppressWarnings("unchecked")
  public CompletableFuture<DistributedSet<E>> buildAsync() {
    ProxyClient<DistributedSetService> proxy = protocol().newProxy(
        name(),
        primitiveType(),
        DistributedSetService.class,
        new ServiceConfig(),
        managementService.getPartitionService());
    return new DistributedSetProxy(proxy, managementService.getPrimitiveRegistry())
        .connect()
        .thenApply(rawSet -> {
          Serializer serializer = serializer();
          AsyncDistributedSet<E> set = new TranscodingAsyncDistributedSet<>(
              rawSet,
              element -> BaseEncoding.base16().encode(serializer.encode(element)),
              string -> serializer.decode(BaseEncoding.base16().decode(string)));

          if (config.getCacheConfig().isEnabled()) {
            set = new CachingAsyncDistributedSet<>(set, config.getCacheConfig());
          }

          if (config.isReadOnly()) {
            set = new UnmodifiableAsyncDistributedSet<>(set);
          }
          return set.sync();
        });
  }
}
