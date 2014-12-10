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
package net.kuujo.copycat.internal;

import net.kuujo.copycat.*;
import net.kuujo.copycat.cluster.Cluster;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Default state machine implementation.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public class DefaultStateMachine<T extends State> implements StateMachine<T> {
  private final Class<T> stateType;
  private T state;
  private final StateLog<List<Object>> log;
  private final InvocationHandler handler = new StateProxyInvocationHandler();
  private StateContext<T> context = new StateContext<T>() {
    private final Map<String, Object> data = new HashMap<>(1024);

    @Override
    public T state() {
      return state;
    }

    @Override
    public StateContext<T> put(String key, Object value) {
      data.put(key, value);
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> U get(String key) {
      return (U) data.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> U remove(String key) {
      return (U) data.remove(key);
    }

    @Override
    public StateContext<T> clear() {
      data.clear();
      return this;
    }

    @Override
    public StateContext<T> transition(T state) {
      DefaultStateMachine.this.state = state;
      return this;
    }
  };

  public DefaultStateMachine(Class<T> stateType, T state, StateLog<List<Object>> log) {
    if (!stateType.isInterface()) {
      throw new IllegalArgumentException("State type must be an interface");
    }
    this.stateType = stateType;
    this.state = state;
    this.log = log;
    registerCommands();
  }

  @Override
  public String name() {
    return log.name();
  }

  @Override
  public Cluster cluster() {
    return log.cluster();
  }

  @Override
  public CopycatState state() {
    return log.state();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <U extends StateProxy> U createProxy(Class<U> type) {
    return (U) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{type}, handler);
  }

  @Override
  public <U> CompletableFuture<U> submit(String command, Object... args) {
    return log.submit(command, Arrays.asList(args));
  }

  @Override
  public CompletableFuture<Void> open() {
    return log.open();
  }

  @Override
  public CompletableFuture<Void> close() {
    return log.close();
  }

  @Override
  public CompletableFuture<Void> delete() {
    return log.delete();
  }

  /**
   * Registers commands on the state log.
   */
  private void registerCommands() {
    for (Method method : stateType.getMethods()) {
      CommandInfo info = method.getAnnotation(CommandInfo.class);
      if (info == null) {
        log.register(method.getName(), createCommand(method));
      } else {
        log.register(info.name().equals("") ? method.getName() : info.name(), createCommand(method), new CommandOptions().withConsistent(info.consistent()).withReadOnly(info.readOnly()));
      }
    }
  }

  /**
   * Creates a state log command for the given method.
   *
   * @param method The method for which to create the state log command.
   * @return The generated state log command.
   */
  private Command<List<Object>, ?> createCommand(Method method) {
    Integer tempIndex = null;
    Class<?>[] paramTypes = method.getParameterTypes();
    for (int i = 0; i < paramTypes.length; i++) {
      if (StateContext.class.isAssignableFrom(paramTypes[i])) {
        tempIndex = i;
      }
    }
    final Integer contextIndex = tempIndex;

    return entry -> {
      Object[] emptyArgs = new Object[entry.size() + (contextIndex != null ? 1 : 0)];
      Object[] args = entry.toArray(emptyArgs);
      if (contextIndex != null) {
        Object lastArg = null;
        for (int i = 0; i < args.length; i++) {
          if (i > contextIndex) {
            args[i] = lastArg;
            lastArg = args[i];
          } else if (i == contextIndex) {
            lastArg = args[i];
            args[i] = context;
          }
        }
      }

      try {
        return method.invoke(state, entry.toArray(new Object[entry.size() + (contextIndex != null ? 1 : 0)]));
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new IllegalStateException(e);
      }
    };
  }

  /**
   * State proxy invocation handler.
   */
  private class StateProxyInvocationHandler implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      Class<?> returnType = method.getReturnType();
      if (returnType == CompletableFuture.class) {
        return submit(method.getName(), args);
      }
      return submit(method.getName(), args).get();
    }
  }

}