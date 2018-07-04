/*
 * Copyright 2017-present Open Networking Foundation
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
package io.atomix.core.countermap.impl;

import com.google.common.io.BaseEncoding;
import io.atomix.core.countermap.AtomicCounterMap;
import io.atomix.primitive.PrimitiveManagementService;
import io.atomix.primitive.proxy.ProxyClient;
import io.atomix.primitive.service.ServiceConfig;
import io.atomix.utils.serializer.Serializer;

import java.util.concurrent.CompletableFuture;

/**
 * Default {@code AtomicCounterMapBuilder}.
 */
public class DefaultAtomicCounterMapBuilder<K> extends AtomicCounterMap.Builder<K> {
  public DefaultAtomicCounterMapBuilder(String name, AtomicCounterMap.Config config, PrimitiveManagementService managementService) {
    super(name, config, managementService);
  }

  @Override
  @SuppressWarnings("unchecked")
  public CompletableFuture<AtomicCounterMap<K>> buildAsync() {
    ProxyClient<AtomicCounterMapService> proxy = protocol().newProxy(
        name(),
        primitiveType(),
        AtomicCounterMapService.class,
        new ServiceConfig(),
        managementService.getPartitionService());
    return new AtomicCounterMapProxy(proxy, managementService.getPrimitiveRegistry())
        .connect()
        .thenApply(map -> {
          Serializer serializer = serializer();
          return new TranscodingAsyncAtomicCounterMap<K, String>(
              map,
              key -> BaseEncoding.base16().encode(serializer.encode(key)),
              string -> serializer.decode(BaseEncoding.base16().decode(string)))
              .sync();
        });
  }
}