package com.spotme.adapters.out.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClasspathRulesConfigAdapterTest {

    @Test
    void loadsKnownVersionFromClasspath() {
        var adapter = new ClasspathRulesConfigAdapter(new ObjectMapper());

        var json = adapter.loadRules("v1.0.0");

        assertEquals("v1.0.0", json.path("version").asText());
        assertEquals(7, json.path("recovery").path("doms").path("severe_threshold").asInt());
    }

    @Test
    void throwsForUnknownVersion() {
        var adapter = new ClasspathRulesConfigAdapter(new ObjectMapper());

        assertThrows(IllegalArgumentException.class, () -> adapter.loadRules("v9.9.9"));
    }
}

