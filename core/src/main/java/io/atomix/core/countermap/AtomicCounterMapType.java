/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.core.countermap;

import io.atomix.core.countermap.impl.DefaultAtomicCounterMapBuilder;
import io.atomix.core.countermap.impl.DefaultAtomicCounterMapService;
import io.atomix.primitive.DistributedPrimitive;
import io.atomix.primitive.PrimitiveManagementService;
import io.atomix.primitive.service.PrimitiveService;
import io.atomix.primitive.service.ServiceConfig;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Atomic counter map primitive type.
 */
public class AtomicCounterMapType<K> implements DistributedPrimitive.Type<AtomicCounterMap.Builder<K>, AtomicCounterMap.Config, AtomicCounterMap<K>> {
  private static final String NAME = "counter-map";
  private static final AtomicCounterMapType INSTANCE = new AtomicCounterMapType();

  /**
   * Returns a new atomic counter map type.
   *
   * @param <K> the key type
   * @return a new atomic counter map type
   */
  @SuppressWarnings("unchecked")
  public static <K> AtomicCounterMapType<K> instance() {
    return INSTANCE;
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public PrimitiveService newService(ServiceConfig config) {
    return new DefaultAtomicCounterMapService();
  }

  @Override
  public AtomicCounterMap.Config newConfig() {
    return new AtomicCounterMap.Config();
  }

  @Override
  public AtomicCounterMap.Builder<K> newBuilder(String name, AtomicCounterMap.Config config, PrimitiveManagementService managementService) {
    return new DefaultAtomicCounterMapBuilder<>(name, config, managementService);
  }

  @Override
  public String toString() {
    return toStringHelper(this)
        .add("name", name())
        .toString();
  }
}