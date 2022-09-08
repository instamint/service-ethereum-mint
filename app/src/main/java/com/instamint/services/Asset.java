package com.instamint.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.annotation.JsonbProperty;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Asset {
    private JsonNode asset;
}
