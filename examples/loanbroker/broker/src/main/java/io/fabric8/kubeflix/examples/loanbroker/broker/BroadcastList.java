/*
 * Copyright (C) 2015 Red Hat, Inc
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

package io.fabric8.kubeflix.examples.loanbroker.broker;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;

import java.util.Collections;
import java.util.List;

public class BroadcastList<T extends Server> implements ServerList<T> {

    private final List<T> list;

    public BroadcastList(ServerList<T> other) {
        this(other.getUpdatedListOfServers());
    }

    public BroadcastList(List<T> list) {
        this.list = Collections.unmodifiableList(list);
    }

    @Override
    public List<T> getInitialListOfServers() {
        return list;
    }

    @Override
    public List<T> getUpdatedListOfServers() {
        return list;
    }

    public String toFixedList() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (Server server : getUpdatedListOfServers()) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(server.getHost()).append(":").append(server.getHostPort());
        }
        return sb.toString();
    }
}
