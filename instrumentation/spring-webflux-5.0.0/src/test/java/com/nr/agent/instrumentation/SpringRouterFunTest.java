/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.internal.HttpServerRule;
import com.newrelic.api.agent.Trace;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.SocketUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.server.HttpServer;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.springframework" })
public class SpringRouterFunTest {

    @ClassRule
    public static HttpServerRule server = new HttpServerRule();

    private static WebClient webClient;

    @BeforeClass
    public static void setup() throws Exception {
        // This is here to prevent reactor.util.ConsoleLogger output from taking over your screen
        System.setProperty("reactor.logging.fallback", "JDK");

        int port = SocketUtils.findAvailableTcpPort();

        HttpServer httpServer = HttpServer.create("0.0.0.0", port);

        final HttpHandler httpHandler = SpringTestHandler.httpHandler(server.getEndPoint());
        httpServer.newHandler(new ReactorHttpHandlerAdapter(new HttpHandler() {
            @Override
            @Trace(dispatcher = true)
            public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
                return httpHandler.handle(request, response);
            }
        })).block();

        final String host = "localhost";
        webClient = WebClient.builder().baseUrl(String.format("http://%s:%d", host, port))
                .clientConnector(new ReactorClientHttpConnector()).build();
    }

    @Test
    public void simplePath() {
        webClient.get().uri("/").exchange().block().bodyToMono(String.class).block();
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(introspector.getTransactionNames().contains("OtherTransaction/Spring/ (GET)"));
    }

    @Test
    public void helloWorldPath() {
        webClient.get().uri("/helloWorld").exchange().block().bodyToMono(String.class).block();
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(introspector.getTransactionNames().contains("OtherTransaction/Spring/helloWorld (GET)"));
    }

    @Test
    public void webClientPath() {
        webClient.get().uri("/web-client").exchange().block().bodyToMono(String.class).block();
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(introspector.getTransactionNames().contains("OtherTransaction/Spring/web-client (GET)"));
    }

    @Test
    public void noMatch() {
        int statusCode = webClient.post()
                .uri("/createUser")
                .contentType(MediaType.TEXT_PLAIN)
                .exchange()
                .block().statusCode().value();
        assertEquals(404, statusCode);
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(msg(introspector), introspector.getTransactionNames().contains("OtherTransaction/Spring/Unknown Route (POST)"));
    }

    @Test
    public void createPersonPath() {
        int block = webClient.post()
                .uri("/person")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .block().statusCode().value();
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(msg(introspector), introspector.getTransactionNames()
                .contains("OtherTransaction/Spring/person (POST)"));
    }

    @Test
    public void nested() {
        assertEquals(200, webClient.get().uri("/language/en-us/nested").exchange().block().statusCode().value());
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(msg(introspector), introspector.getTransactionNames().contains("OtherTransaction/Spring/language/{language}/nested (GET)"));
    }

    @Test
    public void postRegex() {
        final String responseBody = webClient.post().uri("/path/ToNowhere!!!!")
                .contentType(MediaType.APPLICATION_JSON)
                .syncBody("{\"this\": \"isJSON\"}")
                .exchange()
                .block()
                .bodyToMono(String.class)
                .block();

        assertEquals("Got[where] = " + "ToNowhere!!!!", responseBody);
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(msg(introspector), introspector.getTransactionNames().contains("OtherTransaction/Spring/path/{where} (POST)"));
    }

    @Test
    public void queryParam() {
        final String responseBody = webClient.get().uri("/wat/wat/wat?bar=java")
                .exchange()
                .block()
                .bodyToMono(String.class)
                .block();

        assertEquals("query parameter request", responseBody);
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(introspector.getTransactionNames().contains("OtherTransaction/Spring/QueryParameter/bar (GET)"));
    }

    @Test
    public void headers() {
        final String responseBody = webClient.get()
                .uri("/some/other/path")
                .header("SpecialHeader", "productive")
                .exchange()
                .block()
                .bodyToMono(String.class)
                .block();
        assertEquals("Headers request", responseBody);
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(introspector.getTransactionNames().toString(), introspector.getTransactionNames().contains("OtherTransaction/Spring/Unknown Route (GET)"));
    }

    @Test
    public void pathExtension() {
        final String responseBody = webClient.get()
                .uri("favorite.html")
                .exchange()
                .block()
                .bodyToMono(String.class)
                .block();
        assertEquals("Here's your html file", responseBody);
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(introspector.getTransactionNames().contains("OtherTransaction/Spring/PathExtension/html (GET)"));
    }

    @Test
    public void contentType() {
        final int statusCode = webClient.post()
                .uri("/uploadPDF")
                .contentType(MediaType.APPLICATION_PDF)
                .exchange()
                .block()
                .statusCode()
                .value();
        assertEquals(200, statusCode);
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(msg(introspector), introspector.getTransactionNames()
                .contains("OtherTransaction/Spring/uploadPDF (POST)"));
    }

    @Test
    public void resources() {
        final String response = webClient.get()
                .uri("files/numbers.txt")
                .exchange()
                .block()
                .bodyToMono(String.class)
                .block();

        assertEquals("1\n2\n3\n4\n5\n6\n7\n8\n9\n10", response);
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(msg(introspector), introspector.getTransactionNames().contains("OtherTransaction/Spring/files/{*} (GET)"));
    }

    @Test
    public void pathAccept() {
        final String response = webClient.get()
                .uri("/path-to-greatness")
                .accept(MediaType.TEXT_PLAIN)
                .exchange()
                .block()
                .bodyToMono(String.class)
                .block();

        assertEquals("Path to greatness", response);
        final Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount(3000));
        assertTrue(msg(introspector),
                introspector.getTransactionNames().contains("OtherTransaction/Spring/path-to-greatness (GET)"));
    }

    private String msg(Introspector introspector) {
        return "Couldn't find transaction name, but found other transactions: " + introspector.getTransactionNames();
    }
}
