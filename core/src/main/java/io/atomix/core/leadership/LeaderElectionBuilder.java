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
package io.atomix.core.leadership;

import io.atomix.cluster.MemberId;
import io.atomix.primitive.PrimitiveBuilder;
import io.atomix.primitive.PrimitiveManagementService;
import io.atomix.utils.serializer.Namespace;
import io.atomix.utils.serializer.NamespaceConfig;
import io.atomix.utils.serializer.Namespaces;
import io.atomix.utils.serializer.Serializer;

/**
 * Builder for constructing new {@link AsyncLeaderElection} instances.
 */
public abstract class LeaderElectionBuilder<T>
    extends PrimitiveBuilder<LeaderElectionBuilder<T>, LeaderElectionConfig, LeaderElection<T>> {

  public LeaderElectionBuilder(String name, LeaderElectionConfig config, PrimitiveManagementService managementService) {
    super(LeaderElectionType.instance(), name, config, managementService);
  }

  @Override
  public Serializer serializer() {
    Serializer serializer = this.serializer;
    if (serializer == null) {
      NamespaceConfig config = this.config.getNamespaceConfig();
      if (config == null) {
        serializer = Serializer.using(Namespace.builder()
            .register(Namespaces.BASIC)
            .register(MemberId.class)
            .build());
      } else {
        serializer = Serializer.using(Namespace.builder()
            .register(Namespaces.BASIC)
            .register(MemberId.class)
            .register(new Namespace(config))
            .build());
      }
    }
    return serializer;
  }
}
