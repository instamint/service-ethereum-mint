package com.instamint.services.mint.eth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/*
IPFS_BASE_URL = "https://ipfs.io/ipfs/"
INFURA_IPFS_PROJECT_ID= os.environ['INFURA_IPFS_PROJECT_ID']
INFURA_IPFS_PROJECT_SECRET= os.environ['INFURA_IPFS_PROJECT_SECRET']
INFURA_IPFS_API_ADD_URL = "https://ipfs.infura.io:5001/api/v0/add"
 */

public class IPFSService {
    private static String IPFS_BASE_URL = "https://ipfs.io/ipfs/";
    private static String INFURA_IPFS_API_ADD_URL = "https://ipfs.infura.io:5001/api/v0/add";

    public String publishMetadata(String metaData) {
        // Load Infura params
        Dotenv dotenv = Dotenv.configure().load();
        String infuraIPFSProjectID = dotenv.get("INFURA_IPFS_PROJECT_ID");
        String infuraIpfsProjectSecret = dotenv.get("INFURA_IPFS_PROJECT_SECRET");

        // Create HTTP client
        CloseableHttpClient client = HttpClients.createDefault();

        // Prepare to POST and add to Infura a metadata file
        HttpPost post = new HttpPost(INFURA_IPFS_API_ADD_URL);

        // Build auth header
        String auth = String.format("%s:%s",infuraIPFSProjectID,infuraIpfsProjectSecret);
        System.out.println(auth);
        byte[] encodedAuth = Base64.getEncoder().encode(
                auth.getBytes(StandardCharsets.ISO_8859_1));
        String authHeader = "Basic " + new String(encodedAuth);
        post.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

        // Metadata as body attachment to HTTP POST
        StringBody metaBody = new StringBody(metaData, ContentType.APPLICATION_JSON);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addPart("metadata",metaBody);
        HttpEntity entity = builder.build();
        post.setEntity(entity);

        // POST to Infura
        HttpResponse resp = null;
        try {
            resp = client.execute(post);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Process response and obtain CID
        String content = null;
        try {
            content = new BasicResponseHandler().handleResponse(resp);
            System.out.println("Content: " + content);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ObjectMapper mapper = new ObjectMapper();
        InfuraResponse ir = null;
        try {
            ir = mapper.readValue(content, InfuraResponse.class);
            System.out.println("INfura response: " + ir);
            System.out.println("cid: " + ir.getHash());
            System.out.println("name: " + ir.getName());
            System.out.println("size: " + ir.getSize());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        assert ir != null;
        String cid = ir.getHash();


        return cid;
    }

    public void publish(String assetID, Map<String,String> references) throws IOException, InterruptedException {
        Dotenv dotenv = Dotenv.configure().load();
        String infuraIPFSProjectID = dotenv.get("INFURA_IPFS_PROJECT_ID");
        String infuraIpfsProjectSecret = dotenv.get("INFURA_IPFS_PROJECT_SECRET");
        System.out.println(infuraIPFSProjectID);
        System.out.println(infuraIpfsProjectSecret);

        for(Map.Entry<String,String> ref : references.entrySet()) {
            String key = ref.getKey();
            String url = ref.getValue();

            // Get image
            CloseableHttpClient client = HttpClients.createDefault();
            HttpGet req = new HttpGet(url);
            CloseableHttpResponse resp = client.execute(req);
            byte[] image = resp.getEntity().getContent().readAllBytes();
            Files.write(new File(key).toPath(), image);

            // Prepare post w/ auth to Infura
            HttpPost post = new HttpPost(INFURA_IPFS_API_ADD_URL);
            String auth = infuraIPFSProjectID + ":" + infuraIpfsProjectSecret;
            byte[] encodedAuth = Base64.getEncoder().encode(
                    auth.getBytes(StandardCharsets.ISO_8859_1));
            String authHeader = "Basic " + new String(encodedAuth);
            post.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

            // Create part in multipart
            ByteArrayBody bab = new ByteArrayBody(image,key);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addPart(key,bab);
            HttpEntity entity = builder.build();
            post.setEntity(entity);

            // Post
            resp = client.execute(post);

            // Process response and obtain CID
            String content = new BasicResponseHandler().handleResponse(resp);
            Jsonb jsonb = JsonbBuilder.create();
            InfuraResponse ir = jsonb.fromJson(content,InfuraResponse.class);
            System.out.println(ir.getHash());
        }


   }

    public static void main(String[] args) throws IOException, InterruptedException {
        Map<String,String> references = new HashMap<>();
        references.put("lamborghini.png","https://www.lamborghini.com/sites/it-en/files/DAM/lamborghini/facelift_2019/homepage/families-gallery/2022/04_12/family_chooser_tecnica_m.png");
        new IPFSService().publish("",references);
    }
}
