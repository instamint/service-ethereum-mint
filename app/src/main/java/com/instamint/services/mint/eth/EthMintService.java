package com.instamint.services.mint.eth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.consumer.ConsumerRecord;


import java.sql.*;

public class EthMintService {
    private Connection c;
    public EthMintService() {
        Statement stmt = null;

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            c = DriverManager.getConnection("jdbc:postgresql://instamint-db-dev3-do-user-9929701-0.b.db.ondigitalocean.com:25061/InstamintPool?prepareThreshold=0&sslmode=require",
                    "instamint", "WvWUHVlMn7Qxildn");
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public Party getPartyByID(Long id) throws SQLException {
        Statement stmt =  c.createStatement();
        ResultSet rs = stmt.executeQuery( "SELECT id, uuid, hashid, name FROM party where id=" + id);
        if(rs.next()) {
            Party party = new Party();
            party.setId(rs.getLong("id"));
            party.setName(rs.getString("name"));
            party.setHashID(rs.getString("hashid"));
            party.setUuid(rs.getString("uuid"));
            return party;
        } else
            return null;
    }

    public Client getClientByID(Long id) throws SQLException {
        Statement stmt =  c.createStatement();
        ResultSet rs = stmt.executeQuery( "SELECT id,party_id from client where id=" + id);
        if(rs.next()) {
            Client client = new Client();
            client.setId(rs.getLong("id"));
            Party party = getPartyByID(rs.getLong("party_id"));
            client.setParty(party);
            return client;
        } else
            return null;
    }

    public void service(ConsumerRecord<String,String> cr) throws SQLException, ClassNotFoundException {
        // Get asset hash from q for the asset that needs to be minted
        String assetHashID = cr.value();

        // Get the asset details
        Statement stmt = c.createStatement();
        ResultSet rs = stmt.executeQuery( "SELECT * FROM asset where hashid='" + assetHashID + "'");
        if(!rs.next()) return; // need better handling
        Long assetID = rs.getLong("id");
        String assetUUID = rs.getString("uuid");
        Client client = getClientByID(rs.getLong("client_id"));
        Party issuer = getPartyByID(rs.getLong("issuer_id"));
        Party owner = getPartyByID(rs.getLong("owner_id"));
        Party custodian = getPartyByID(rs.getLong("custodian_id"));
        String mintRequestJSON = rs.getString("mint_requestjson");

        // Pull the Designer asset type metadata
        Asset asset = null;
        JsonNode node = null;
        ObjectMapper mapper = new ObjectMapper();
        try {

            asset = mapper.readValue(mintRequestJSON, Asset.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        JsonNode assetMetaData = asset.getAsset();

        // Construct the final metadata
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("issuerID",issuer.getHashID());
        metadata.put("clientID",client.getParty().getHashID());
        metadata.putIfAbsent("asset",assetMetaData);
        System.out.println("Final metadata: " + metadata.toString());

        // Publish metadata to IPFS
        String metadataCID = new IPFSService().publishMetadata(metadata.toString());
        System.out.println("metadata CID: "+ metadataCID);
        rs.close();
        stmt.close();


        // Update asset table with metadata CID
        String updateCIDSQL = String.format("UPDATE asset SET metadatacid='%s' where id=%d",metadataCID,assetID);
        c.createStatement().executeUpdate( updateCIDSQL);
    }
}
