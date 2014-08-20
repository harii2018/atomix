/*
 * Copyright 2014 the original author or authors.
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
package net.kuujo.copycat.cluster;

import net.kuujo.copycat.protocol.ProtocolInstance;

/**
 * Cluster member info.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public interface Member {

  /**
   * Returns the member name.
   *
   * @return The member name.
   */
  String name();

  /**
   * Returns the member uri.
   *
   * @return The member uri.
   */
  String uri();

  /**
   * Returns the member's protocol instance.
   *
   * @return The member's protocol instance.
   */
  ProtocolInstance protocol();

}
