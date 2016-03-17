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

package io.fabric8.kubeflix.examples.helloribbon;

import com.netflix.ribbon.ClientOptions;
import com.netflix.ribbon.Ribbon;
import com.netflix.ribbon.http.HttpRequestTemplate;
import com.netflix.ribbon.http.HttpResourceGroup;
import io.fabric8.kubeflix.ribbon.KubernetesClientConfig;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Action1;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class HelloRibbonServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelloRibbonServlet.class);

    private HttpResourceGroup group = Ribbon.createHttpResourceGroupBuilder("hello-hystrix")
            .withClientOptions(ClientOptions.from(new KubernetesClientConfig())).build();

    private HttpRequestTemplate<ByteBuf> template = group.newTemplateBuilder("HelloRibbon")
            .withMethod("GET")
            .withUriTemplate("/hello")
            .build();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setContentType("text/html");
        final PrintWriter out = resp.getWriter();
        template.requestBuilder()
                .build()
                .toObservable()
                .toBlocking().forEach(new Action1<ByteBuf>() {
            @Override
            public void call(ByteBuf buffer) {
                byte[] bytes = new byte[buffer.readableBytes()];
                buffer.readBytes(bytes);
                String next = new String(bytes);
                out.println("<h1>" + next + "</h1>");
            }
        });
    }
}
