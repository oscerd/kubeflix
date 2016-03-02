package io.fabric8.kubeflix.examples.helloribbon;

import com.netflix.client.config.IClientConfig;
import com.netflix.client.config.IClientConfigKey;
import com.netflix.ribbon.ClientOptions;
import com.netflix.ribbon.Ribbon;
import com.netflix.ribbon.http.HttpRequestTemplate;
import com.netflix.ribbon.http.HttpResourceGroup;
import io.fabric8.kubeflix.ribbon.KubernetesServerList;
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

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");

        final PrintWriter out = resp.getWriter();
        out.println("<h1>Load Balance Targets:</h1>");

        IClientConfig config = IClientConfig.Builder.newBuilder().withDefaultValues().build()
                .set(IClientConfigKey.Keys.NIWSServerListClassName, KubernetesServerList.class.getName());

        HttpResourceGroup group = Ribbon.createHttpResourceGroupBuilder("hello-hystrix")
                .withClientOptions(ClientOptions.from(config)).build();

        HttpRequestTemplate<ByteBuf> template = group.newTemplateBuilder("HelloRibbon")
                .withMethod("GET")
                .withUriTemplate("/hello")
                .build();

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
