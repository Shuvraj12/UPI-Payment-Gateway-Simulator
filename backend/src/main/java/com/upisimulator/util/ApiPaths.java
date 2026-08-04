package com.upisimulator.util;

/**
 * Central place for API path prefixes.
 * <p>
 * Deliberately used as an explicit constant on each {@code @RequestMapping}
 * instead of {@code server.servlet.context-path}. Both approaches produce the
 * same URLs, but a global context-path adds a layer that MockMvc, Swagger,
 * and error pages all have to be reasoned about separately. Repeating one
 * constant is a smaller cost than that ambiguity, especially across a
 * 14-phase project with a growing number of controllers.
 */
public final class ApiPaths {

    public static final String BASE = "/api/v1";

    private ApiPaths() {
    }

}
