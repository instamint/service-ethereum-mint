package com.instamint.services.mint.eth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.instamint.services.Party;
import com.instamint.services.Asset;
import com.instamint.services.Client;
import com.instamint.services.store.ipfs.IPFSService;
import io.github.cdimascio.dotenv.Dotenv;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/*
*
* {
    "title": "Asset Metadata",
    "type": "object",
    "properties": {
        "name": {
            "type": "string",
            "description": "Identifies the asset to which this NFT represents"
        },
        "description": {
            "type": "string",
            "description": "Describes the asset to which this NFT represents"
        },
        "image": {
            "type": "string",
            "description": "A URI pointing to a resource with mime type image/* representing the asset to which this NFT represents. Consider making any images at a width between 320 and 1080 pixels and aspect ratio between 1.91:1 and 4:5 inclusive."
        }
    }
}
*
* */
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class EthMintService {
    private Connection c;
    private IPFSService ipfs = new IPFSService();
    public EthMintService() {
        Statement stmt = null;
        Dotenv dotenv = Dotenv.configure().load();
        String dbPassword = dotenv.get("DB_PASSWORD");

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            c = DriverManager.getConnection("jdbc:postgresql://instamint.network:5432/instamint?prepareThreshold=0",
                    "instamint", dbPassword);
        } catch (SQLException e) {
            e.printStackTrace();
        }

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
        System.out.println(asset.getAssetType());
        if(asset.getAssetType().equals("erc721-image-1")) {
            System.out.println("its an erc721");
            String imageURL = assetMetaData.get("image").asText();
            System.out.println(imageURL);
            Map<String,String> ref = new HashMap<>(1);
            ref.put("image",imageURL);
            try {
                ref = ipfs.publish(ref);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            String imageCID = ref.get("image");
            // Construct the final metadata
            ObjectNode metadata = mapper.createObjectNode();
            metadata.put("title",assetMetaData.get("title").asText());
            metadata.put("name",assetMetaData.get("name").asText());
            metadata.put("description",assetMetaData.get("description").asText());
            metadata.put("image","https://ipfs.io/ipfs/"+imageCID);

/*
            metadata.put("issuerID",issuer.getHashID());
            metadata.put("clientID",client.getParty().getHashID());
            metadata.putIfAbsent("asset",assetMetaData);

 */
            System.out.println("Final metadata: " + metadata.toPrettyString());

            // Publish metadata to IPFS
            String metadataCID = ipfs.publishMetadata(metadata.toString());
            System.out.println("metadata CID: "+ metadataCID);
            rs.close();
            stmt.close();

            String metadataURI = "https://ipfs.io/ipfs/" + metadataCID;
            // Update asset table with metadata CID
            String updateCIDSQL = String.format("UPDATE asset SET metadatacid='%s', mint_completed_status=true, metadatauri='%s' where id=%d",metadataCID,metadataURI,assetID);
            c.createStatement().executeUpdate( updateCIDSQL);
        } else {
            System.out.println("It's not");
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


}
