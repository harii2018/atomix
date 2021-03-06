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

package io.atomix.core.treemap.impl;

import com.google.common.collect.Maps;
import io.atomix.core.collection.AsyncDistributedCollection;
import io.atomix.core.collection.impl.TranscodingAsyncDistributedCollection;
import io.atomix.core.map.AtomicMapEvent;
import io.atomix.core.map.AtomicMapEventListener;
import io.atomix.core.map.impl.MapUpdate;
import io.atomix.core.set.AsyncDistributedSet;
import io.atomix.core.set.impl.TranscodingAsyncDistributedSet;
import io.atomix.core.transaction.TransactionId;
import io.atomix.core.transaction.TransactionLog;
import io.atomix.core.treemap.AsyncAtomicTreeMap;
import io.atomix.core.treemap.AtomicTreeMap;
import io.atomix.primitive.impl.DelegatingAsyncPrimitive;
import io.atomix.utils.concurrent.Futures;
import io.atomix.utils.time.Versioned;

import java.time.Duration;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An {@code AsyncConsistentTreeMap} that maps its operations to operations on
 * a differently typed {@code AsyncConsistentTreeMap} by transcoding operation
 * inputs and outputs.
 *
 * @param <V2> value type of other map
 * @param <V1> value type of this map
 */
public class TranscodingAsyncAtomicTreeMap<V1, V2> extends DelegatingAsyncPrimitive implements AsyncAtomicTreeMap<V1> {
  private final AsyncAtomicTreeMap<V2> backingMap;
  private final Function<V2, V1> valueDecoder;
  private final Function<V1, V2> valueEncoder;
  private final Function<Versioned<V2>, Versioned<V1>> versionedValueDecoder;
  private final Function<Versioned<V1>, Versioned<V2>> versionedValueEncoder;
  private final Function<Map.Entry<String, Versioned<V2>>, Map.Entry<String, Versioned<V1>>> entryDecoder;
  private final Function<Map.Entry<String, Versioned<V1>>, Map.Entry<String, Versioned<V2>>> entryEncoder;
  private final Map<AtomicMapEventListener<String, V1>, InternalBackingAtomicMapEventListener> listeners = Maps.newIdentityHashMap();

  public TranscodingAsyncAtomicTreeMap(
      AsyncAtomicTreeMap<V2> backingMap,
      Function<V1, V2> valueEncoder,
      Function<V2, V1> valueDecoder) {
    super(backingMap);
    this.backingMap = backingMap;
    this.valueEncoder = v -> v == null ? null : valueEncoder.apply(v);
    this.valueDecoder = v -> v == null ? null : valueDecoder.apply(v);
    this.versionedValueDecoder = v -> v == null ? null : v.map(valueDecoder);
    this.versionedValueEncoder = v -> v == null ? null : v.map(valueEncoder);
    this.entryDecoder = e -> e == null ? null : Maps.immutableEntry(e.getKey(), versionedValueDecoder.apply(e.getValue()));
    this.entryEncoder = e -> e == null ? null : Maps.immutableEntry(e.getKey(), versionedValueEncoder.apply(e.getValue()));
  }

  @Override
  public CompletableFuture<String> firstKey() {
    return backingMap.firstKey();
  }

  @Override
  public CompletableFuture<String> lastKey() {
    return backingMap.lastKey();
  }

  @Override
  public CompletableFuture<Map.Entry<String, Versioned<V1>>> ceilingEntry(String key) {
    return backingMap.ceilingEntry(key)
        .thenApply(entry -> entry != null ? Maps.immutableEntry(entry.getKey(), versionedValueDecoder.apply(entry.getValue())) : null);
  }

  @Override
  public CompletableFuture<Map.Entry<String, Versioned<V1>>> floorEntry(String key) {
    return backingMap.floorEntry(key)
        .thenApply(entry -> entry != null ? Maps.immutableEntry(entry.getKey(), versionedValueDecoder.apply(entry.getValue())) : null);
  }

  @Override
  public CompletableFuture<Map.Entry<String, Versioned<V1>>> higherEntry(String key) {
    return backingMap.higherEntry(key)
        .thenApply(entry -> entry != null ? Maps.immutableEntry(entry.getKey(), versionedValueDecoder.apply(entry.getValue())) : null);
  }

  @Override
  public CompletableFuture<Map.Entry<String, Versioned<V1>>> lowerEntry(String key) {
    return backingMap.lowerEntry(key)
        .thenApply(entry -> entry != null ? Maps.immutableEntry(entry.getKey(), versionedValueDecoder.apply(entry.getValue())) : null);
  }

  @Override
  public CompletableFuture<Map.Entry<String, Versioned<V1>>> firstEntry() {
    return backingMap.firstEntry()
        .thenApply(entry -> entry != null ? Maps.immutableEntry(entry.getKey(), versionedValueDecoder.apply(entry.getValue())) : null);
  }

  @Override
  public CompletableFuture<Map.Entry<String, Versioned<V1>>> lastEntry() {
    return backingMap.lastEntry()
        .thenApply(entry -> entry != null ? Maps.immutableEntry(entry.getKey(), versionedValueDecoder.apply(entry.getValue())) : null);
  }

  @Override
  public CompletableFuture<String> lowerKey(String key) {
    return backingMap.lowerKey(key);
  }

  @Override
  public CompletableFuture<String> floorKey(String key) {
    return backingMap.floorKey(key);
  }

  @Override
  public CompletableFuture<String> ceilingKey(String key) {
    return backingMap.ceilingKey(key);
  }

  @Override
  public CompletableFuture<String> higherKey(String key) {
    return backingMap.higherKey(key);
  }

  @Override
  public CompletableFuture<NavigableSet<String>> navigableKeySet() {
    return backingMap.navigableKeySet();
  }

  @Override
  public CompletableFuture<NavigableMap<String, V1>> subMap(
      String upperKey, String lowerKey, boolean inclusiveUpper, boolean inclusiveLower) {
    throw new UnsupportedOperationException("This operation is not yet supported.");
  }

  @Override
  public String name() {
    return backingMap.name();
  }

  @Override
  public CompletableFuture<Integer> size() {
    return backingMap.size();
  }

  @Override
  public CompletableFuture<Boolean> containsKey(String key) {
    return backingMap.containsKey(key);
  }

  @Override
  public CompletableFuture<Boolean> containsValue(V1 value) {
    return backingMap.containsValue(valueEncoder.apply(value));
  }

  @Override
  public CompletableFuture<Versioned<V1>> get(String key) {
    return backingMap.get(key).thenApply(versionedValueDecoder);
  }

  @Override
  public CompletableFuture<Map<String, Versioned<V1>>> getAllPresent(Iterable<String> keys) {
    return backingMap.getAllPresent(keys)
        .thenApply(map -> Maps.transformValues(map, versionedValueDecoder::apply));
  }

  @Override
  public CompletableFuture<Versioned<V1>> getOrDefault(String key, V1 defaultValue) {
    return backingMap.getOrDefault(key, valueEncoder.apply(defaultValue)).thenApply(versionedValueDecoder);
  }

  @Override
  public CompletableFuture<Versioned<V1>> computeIf(
      String key,
      Predicate<? super V1> condition,
      BiFunction<? super String, ? super V1, ? extends V1> remappingFunction) {
    try {
      return backingMap.computeIf(
          key,
          v -> condition.test(valueDecoder.apply(v)),
          (k, v) -> valueEncoder.apply(remappingFunction.apply(key, valueDecoder.apply(v))))
          .thenApply(versionedValueDecoder);
    } catch (Exception e) {
      return Futures.exceptionalFuture(e);
    }
  }

  @Override
  public CompletableFuture<Versioned<V1>> put(String key, V1 value, Duration ttl) {
    return backingMap.put(key, valueEncoder.apply(value), ttl)
        .thenApply(versionedValueDecoder);
  }

  @Override
  public CompletableFuture<Versioned<V1>> putAndGet(String key, V1 value, Duration ttl) {
    return backingMap.putAndGet(key, valueEncoder.apply(value), ttl)
        .thenApply(versionedValueDecoder);
  }

  @Override
  public CompletableFuture<Versioned<V1>> remove(String key) {
    return backingMap.remove(key).thenApply(versionedValueDecoder);
  }

  @Override
  public CompletableFuture<Void> clear() {
    return backingMap.clear();
  }

  @Override
  public AsyncDistributedSet<String> keySet() {
    return backingMap.keySet();
  }

  @Override
  public AsyncDistributedCollection<Versioned<V1>> values() {
    return new TranscodingAsyncDistributedCollection<>(backingMap.values(), versionedValueEncoder, versionedValueDecoder);
  }

  @Override
  public AsyncDistributedSet<Map.Entry<String, Versioned<V1>>> entrySet() {
    return new TranscodingAsyncDistributedSet<>(backingMap.entrySet(), entryEncoder, entryDecoder);
  }

  @Override
  public CompletableFuture<Versioned<V1>> putIfAbsent(String key, V1 value, Duration ttl) {
    return backingMap.putIfAbsent(key, valueEncoder.apply(value), ttl)
        .thenApply(versionedValueDecoder);
  }

  @Override
  public CompletableFuture<Boolean> remove(String key, V1 value) {
    return backingMap.remove(key, valueEncoder.apply(value));
  }

  @Override
  public CompletableFuture<Boolean> remove(String key, long version) {
    return backingMap.remove(key, version);
  }

  @Override
  public CompletableFuture<Versioned<V1>> replace(String key, V1 value) {
    return backingMap.replace(key, valueEncoder.apply(value))
        .thenApply(versionedValueDecoder);
  }

  @Override
  public CompletableFuture<Boolean> replace(String key, V1 oldValue, V1 newValue) {
    return backingMap.replace(key, valueEncoder.apply(oldValue),
        valueEncoder.apply(newValue));
  }

  @Override
  public CompletableFuture<Boolean> replace(String key, long oldVersion, V1 newValue) {
    return backingMap.replace(key, oldVersion,
        valueEncoder.apply(newValue));
  }

  @Override
  public CompletableFuture<Void> addListener(AtomicMapEventListener<String, V1> listener, Executor executor) {
    InternalBackingAtomicMapEventListener backingMapEventListener = listeners.computeIfAbsent(listener,
        k -> new InternalBackingAtomicMapEventListener(listener));
    return backingMap.addListener(backingMapEventListener, executor);
  }

  @Override
  public CompletableFuture<Void> removeListener(AtomicMapEventListener<String, V1> listener) {
    InternalBackingAtomicMapEventListener backingMapEventListener = listeners.remove(listener);
    if (backingMapEventListener == null) {
      return CompletableFuture.completedFuture(null);
    } else {
      return backingMap.removeListener(backingMapEventListener);
    }
  }

  @Override
  public CompletableFuture<Boolean> prepare(TransactionLog<MapUpdate<String, V1>> transactionLog) {
    throw new UnsupportedOperationException("This operation is not yet supported.");
  }

  @Override
  public CompletableFuture<Void> commit(TransactionId transactionId) {
    throw new UnsupportedOperationException("This operation is not yet supported.");
  }

  @Override
  public CompletableFuture<Void> rollback(TransactionId transactionId) {
    throw new UnsupportedOperationException("This operation is not yet supported.");
  }

  @Override
  public AtomicTreeMap<V1> sync(Duration operationTimeout) {
    return new BlockingAtomicTreeMap<>(this, operationTimeout.toMillis());
  }

  private class InternalBackingAtomicMapEventListener implements AtomicMapEventListener<String, V2> {
    private final AtomicMapEventListener<String, V1> listener;

    InternalBackingAtomicMapEventListener(AtomicMapEventListener<String, V1> listener) {
      this.listener = listener;
    }

    @Override
    public void event(AtomicMapEvent<String, V2> event) {
      listener.event(new AtomicMapEvent<>(
          event.type(),
          event.name(),
          event.key(),
          event.newValue() != null ? event.newValue().map(valueDecoder) : null,
          event.oldValue() != null ? event.oldValue().map(valueDecoder) : null));
    }
  }
}
