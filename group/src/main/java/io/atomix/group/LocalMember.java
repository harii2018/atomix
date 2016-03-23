/*
 * Copyright 2015 the original author or authors.
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
 * limitations under the License
 */
package io.atomix.group;

import io.atomix.group.connection.ConnectionController;
import io.atomix.group.connection.LocalConnection;
import io.atomix.group.state.GroupCommands;
import io.atomix.group.task.LocalTaskQueue;
import io.atomix.group.task.TaskQueueController;
import io.atomix.group.util.Submitter;

import java.util.concurrent.CompletableFuture;

/**
 * A {@link DistributedGroup} member representing a member of the group controlled by the
 * local process.
 * <p>
 * Local group members can only be acquired by {@link DistributedGroup#join() joining} a membership
 * group. Local members provide the interface necessary to set member properties, receive messages,
 * and react to the election of the member as the group leader.
 * <p>
 * To receive messages sent to the joined member of the group, register a message consumer. Messages
 * sent to the member are associated with a {@link String} topic, and separate handlers can be registered
 * for each topic supported by the local member:
 * <pre>
 *   {@code
 *   LocalGroupMember member = group.join().get();
 *   member.onMessage("foo", message -> System.out.println("received: " + message));
 *   }
 * </pre>
 *
 * @author <a href="http://github.com/kuujo>Jordan Halterman</a>
 */
public class LocalMember extends GroupMember {
  final ConnectionController connection;

  LocalMember(GroupMemberInfo info, MembershipGroup group, Submitter submitter) {
    super(info, group, submitter, new TaskQueueController(new LocalTaskQueue(info.memberId(), group, submitter)));
    this.connection = new ConnectionController(new LocalConnection(info.memberId(), info.address(), group.connections));
  }

  @Override
  public LocalTaskQueue tasks() {
    return (LocalTaskQueue) tasks.queue();
  }

  @Override
  public LocalConnection connection() {
    return connection.connection();
  }

  /**
   * Leaves the membership group.
   * <p>
   * When this member leaves the membership group, the membership lists of this and all other instances
   * in the group are guaranteed to be updated <em>before</em> the {@link CompletableFuture} returned by
   * this method is completed. Once this instance has left the group, the returned future will be completed.
   * <p>
   * This method returns a {@link CompletableFuture} which can be used to block until the operation completes
   * or to be notified in a separate thread once the operation completes. To block until the operation completes,
   * use the {@link CompletableFuture#join()} method to block the calling thread:
   * <pre>
   *   {@code
   *   member.leave().join();
   *   }
   * </pre>
   * Alternatively, to execute the operation asynchronous and be notified once the lock is acquired in a different
   * thread, use one of the many completable future callbacks:
   * <pre>
   *   {@code
   *   member.leave().thenRun(() -> System.out.println("Left the group!")));
   *   }
   * </pre>
   *
   * @return A completable future to be completed once the member has left.
   */
  public CompletableFuture<Void> leave() {
    return group.submit(new GroupCommands.Leave(memberId)).whenComplete((result, error) -> {
      group.members.remove(memberId);
    });
  }

}
