package io.fabric8.kubeflix.examples.hellohystrix;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class HelloHystrixServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // Set response content type
        resp.setContentType("text/html");


        // Actual logic goes here.
        PrintWriter out = resp.getWriter();
        out.println("<h1>" + new HelloCommand().execute() + "</h1>");
    }
}
