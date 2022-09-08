package com.instamint;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // Need this so that not all properties are expected in the inbound JSON
public class AssetMintRequest {
    private String issuerID;
    private String xref;
    private String chain;
    private String assetType;
    private String contract;
    private String strategy;
    private JsonNode asset;
    private String note;
    private String assetDescription;

    private Long assetID;
    private String contractAddress;
    private Long transactionHeaderID;
    private String hashID;
    private Long chainID;

}
