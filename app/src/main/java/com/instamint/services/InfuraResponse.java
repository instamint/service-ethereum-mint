package com.instamint.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.json.bind.annotation.JsonbProperty;
import lombok.Data;

@Data
public class InfuraResponse {
    @JsonProperty ("Name") private String name;
    @JsonProperty("Hash") private String hash;
    @JsonProperty("Size") private String size;
}
