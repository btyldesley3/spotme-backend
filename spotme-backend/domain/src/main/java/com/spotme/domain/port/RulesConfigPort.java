package com.spotme.domain.port;

import com.fasterxml.jackson.databind.JsonNode;

/** Loads progression policy configuration (e.g., from classpath or S3). */
public interface RulesConfigPort {
    JsonNode loadRules(String version);
}
