package com.instamint.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instamint.contracts.Insta1;
import com.instamint.contracts.Insta2;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.*;
import org.web3j.tx.gas.DefaultGasProvider;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;


public class EthereumTestClient {

    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
        // Insta1 contract address on Goerli, custodian addresss (owner) and private key
        String erc721contractAddress = "0x1febB2aa02e0d93cBF1469F494D6e3dcFa1B24B9"; // erc721 on goerli
        String erc1155contractAddress = "0xBf788Ca620bC50ec03ccC8F4B7f93b4D1192eEbE";
        String erc721MainnetcontractAddress = "0xa993749323E2A8B18f7D4Ef541D6A1a89Cd92888";
        String custodianAddress = "0x9DF89C92d9aE42DDB1FDaAC71Bb3F678C93C271b";
        String pk = "61150d784c1c61f5e9364f62a633b1cef07a2f99fae9eb648bb01c7e7be883b6";

        // Create web3 client
        Web3j w3 = Web3j.build(new HttpService("https://goerli.infura.io/v3/f63ce9131ccf45ae96d92a1ba87c053f"));

        // Get latest block number
        EthBlockNumber ethBlockNumber = w3.ethBlockNumber().sendAsync().get();
        System.out.println(ethBlockNumber.getBlockNumber());


//        // Create HTTP client
//        CloseableHttpClient client = HttpClients.createDefault();

        // Get ABI of contract INSTA1 via Etherscan
//        String abiEndpointURL = "https://api-goerli.etherscan.io/api?module=contract&action=getabi&address="+erc721contractAddress+"&apiKey=JN4S1EPTSKID8J5IY9B2UYYJ9J56TDU5D2";
//        HttpGet get = new HttpGet(abiEndpointURL); // what is sneakthrows?
//        CloseableHttpResponse response = client.execute(get);
//        System.out.println(response);
//        System.out.println(response.getEntity());
//        HttpEntity entity = response.getEntity();
//        Header encodingHeader = entity.getContentEncoding();
//        ObjectMapper mapper = new ObjectMapper();
//        JsonNode abi = mapper.readTree(EntityUtils.toString(entity));
//        abi = abi.get("result");
//        System.out.println(abi.asText());
//        Files.write(new File("erc1155.json").toPath(), Collections.singleton(abi.asText()));

        // Get transaction count for account
        EthGetTransactionCount transactionCount = w3.ethGetTransactionCount(custodianAddress, DefaultBlockParameter.valueOf("latest")).sendAsync().get();
        System.out.println("Tx count: " + transactionCount.getTransactionCount());

        // Get balance
        EthGetBalance balance = w3.ethGetBalance(custodianAddress, DefaultBlockParameter.valueOf("latest")).sendAsync().get();
        System.out.println("Balance: " + balance.getBalance());
        Credentials cred = Credentials.create(pk);

        // Get Insta1's bytecode
        TransactionManager txMan = new RawTransactionManager(w3,cred, ChainIdLong.ETHEREUM_CLASSIC_TESTNET);
        EthGetCode code = txMan.getCode(erc1155contractAddress, DefaultBlockParameter.valueOf("latest"));
        System.out.println(code.getCode());


        //Contract contract = new Contract(json,contractAddress,w3,cred,new DefaultGasProvider());

        // Use statically typed Insta1 wrapper to get tokenURI for a token ID
        System.out.println("Insta1");
        Insta1 insta1 = Insta1.load(erc721contractAddress,w3,cred,new DefaultGasProvider());
        String uri = insta1.tokenURI(BigInteger.valueOf(1)).sendAsync().get();
        System.out.println("uri: "+ uri);
        String owner = insta1.owner().sendAsync().get();
        System.out.println("owner: " + owner);


        // Use Functions to invoke tokenURI function
        System.out.println("Function");
        List<Type> input = List.of(new Uint(BigInteger.valueOf(2)));
        List<TypeReference<?>> output = List.of(new TypeReference<Utf8String>() { });
        Function function = new Function ("tokenURI",input,output);
        String encodedFunction = FunctionEncoder.encode(function);
        Transaction functionCallTransaction = Transaction.createEthCallTransaction(custodianAddress,erc721contractAddress,encodedFunction);
        EthCall call = w3.ethCall(functionCallTransaction, DefaultBlockParameterName.LATEST).sendAsync().get();
        List<Type> someTypes = FunctionReturnDecoder.decode(call.getValue(),function.getOutputParameters());
        System.out.println(someTypes.get(0));
//
        // Use statically typed Insta2 wrapper to get tokenURI for a token ID
        System.out.println("Insta2");
        Insta2 insta2 = Insta2.load(erc1155contractAddress,w3,cred,new DefaultGasProvider());

        uri = insta2.uri(BigInteger.valueOf(2)).sendAsync().get();
        System.out.println("uri: "+ uri);
        owner = insta2.owner().sendAsync().get();
        System.out.println("owner: " + owner);
//
//        // Use statically typed Insta1 wrapper to get tokenURI for a token ID
//        System.out.println("Insta1 Prod");
//        Insta1 insta1prod = Insta1.load(erc721MainnetcontractAddress,w3,cred,new DefaultGasProvider());
//        uri = insta1prod.tokenURI(BigInteger.valueOf(10)).sendAsync().get();
//        System.out.println("uri: "+ uri);
//        owner = insta1prod.owner().sendAsync().get();
//        System.out.println("owner: " + owner);



    }
}
