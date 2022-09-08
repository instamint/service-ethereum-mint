package com.instamint.services.mint.eth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.instamint.AssetMintRequest;
import com.instamint.contracts.Insta1;
import com.instamint.services.Party;
import com.instamint.services.Asset;
import com.instamint.services.Client;
import com.instamint.services.store.ipfs.IPFSService;
import io.github.cdimascio.dotenv.Dotenv;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.web3j.abi.datatypes.Int;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;

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
import java.math.BigInteger;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class EthMintService {
    private Connection c;
    private IPFSService ipfs = new IPFSService();
    private Dotenv env = Dotenv.configure().load();

    public EthMintService() {
        Statement stmt = null;
        String dbPassword = env.get("DB_PASSWORD");

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            c = DriverManager.getConnection("jdbc:postgresql://instamint.network:5432/jamiel?prepareThreshold=0",
                    "instamint", dbPassword);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
    public void service(ConsumerRecord<String,String> cr) throws SQLException, ClassNotFoundException {
        ObjectMapper mapper = new ObjectMapper();
        AssetMintRequest assetMintRequest = mapper.convertValue(cr.value(), AssetMintRequest.class);

        // Get asset hash from q for the asset that needs to be minted
        String assetHashID = assetMintRequest.getHashID();

        // Get the asset details
        Statement stmt = c.createStatement();
        ResultSet rs = stmt.executeQuery( "SELECT * FROM asset where hashid='" + assetHashID + "'");
        if(!rs.next()) return; // need better handling
        Long assetID = rs.getLong("id");
//        String assetUUID = rs.getString("uuid");
//        Client client = getClientByID(rs.getLong("client_id"));
//        Party issuer = getPartyByID(rs.getLong("issuer_id"));
//        Party owner = getPartyByID(rs.getLong("owner_id"));
//        Party custodian = getPartyByID(rs.getLong("custodian_id"));
        //String mintRequestJSON = rs.getString("mint_requestjson");
        //String chainID = rs.getString("chain_id");
        System.out.println("Minting on chain ID: " + assetMintRequest.getChain());

        // Pull the Designer asset type metadata
//        Asset asset = null;
//        JsonNode node = null;
//        try {
//
//            asset = mapper.readValue(mintRequestJSON, Asset.class);
//        } catch (JsonProcessingException e) {
//            e.printStackTrace();
//
//        }
        JsonNode assetMetaData = assetMintRequest.getAsset();
//        System.out.println(asset.getAssetType());
//        System.out.println("contract: " + asset.getContract());

        if(assetMintRequest.getAssetType().equals("single-image-with-erc721-metadata")) {
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
            String updateCIDSQL = String.format("UPDATE asset SET metadatacid='%s', metadatauri='%s' where id=%d",metadataCID,metadataURI,assetID);
            c.createStatement().executeUpdate( updateCIDSQL);

            // Mint the token
            if(assetMintRequest.getContract().equalsIgnoreCase("insta1")) {
                System.out.println("Insta1");

                // Lookup contract address based on contract short name and chain

//                rs = c.createStatement().executeQuery( "SELECT address FROM ethereum_contract where chain_id='" + chainID + "' and short_name='insta1'");
//                rs.next();
                System.out.println("Contract address is: " + assetMintRequest.getContractAddress());
//                String contractAddress = rs.getString("address");
                Web3j w3 = Web3j.build(new HttpService("https://goerli.infura.io/v3/f63ce9131ccf45ae96d92a1ba87c053f"));
                String pk = env.get("PK");
                Credentials cred = Credentials.create(pk);
                Insta1 insta1 = Insta1.load(assetMintRequest.getContractAddress(),w3,cred,new DefaultGasProvider());
                try {
                    TransactionReceipt tr = insta1.safeMint("0x9DF89C92d9aE42DDB1FDaAC71Bb3F678C93C271b")
                            .sendAsync().get();// TODO: pull from db who the owner of token is - also how to specify gas?
                    System.out.println(tr);
                    System.out.println("tx index" + tr.getTransactionIndex());
                    System.out.println("bnum" + tr.getBlockNumber());
                    System.out.println("bhash" + tr.getBlockHash());
                    System.out.println("contractadd" + tr.getContractAddress());
                    System.out.println("cum gas used" + tr.getCumulativeGasUsed());
                    System.out.println("from" + tr.getFrom());
                    System.out.println("gas used" + tr.getGasUsed());
                    System.out.println("status" + tr.getStatus());
                    System.out.println("to" + tr.getTo());
                    System.out.println("root" + tr.getRoot());
                    System.out.println("tx hash" + tr.getTransactionHash());
                    for(Log l: tr.getLogs()) {
                        System.out.println("Log index: " + l.getLogIndex());
                        for (String s: l.getTopics()) {
                            System.out.println("--- " + s);
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

            }
        } else {
            System.out.println("It's not");
        }

        System.out.println("Minting complete");
    }

    public void insertTransactionDetail(BigInteger txhID, TransactionReceipt tr) {
        System.out.println(tr);
        BigInteger transactionIndex = tr.getTransactionIndex();
        BigInteger blockNumber = tr.getBlockNumber();
        String blockHash = tr.getBlockHash();
        String contractAddress = tr.getContractAddress();
        BigInteger cumulativeGasUsed = tr.getCumulativeGasUsed();
        String from = tr.getFrom();
        BigInteger gasUsed = tr.getGasUsed();
        String status = tr.getStatus();
        String to = tr.getTo();
        String root = tr.getRoot();
        String transactionHash = tr.getTransactionHash();

        String sql = "INSERT INTO ETHEREUM_TRANSACTION () VALUES (";
    }
    public Party getPartyByID(Long id) throws SQLException {
        Statement stmt =  c.createStatement();
        ResultSet rs = stmt.executeQuery( "SELECT id, uuid, name FROM party where id=" + id);
        if(rs.next()) {
            Party party = new Party();
            party.setId(rs.getLong("id"));
            party.setName(rs.getString("name"));
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
