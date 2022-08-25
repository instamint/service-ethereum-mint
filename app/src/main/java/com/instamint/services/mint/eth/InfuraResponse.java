package com.instamint.services.mint.eth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.json.bind.annotation.JsonbProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InfuraResponse {
    @JsonProperty ("Name") private String name;
    @JsonProperty("Hash") private String hash;
    @JsonProperty("Size") private String size;
}
