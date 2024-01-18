
package com.osc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.collection.ISet;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.osc.entity.Categories;
import com.osc.entity.UserData;
import com.osc.product.*;
import com.osc.repository.CategoryRepository;
import com.osc.repository.ProductRepository;
import com.osc.repository.UserDataRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@GrpcService
public class ProductService extends ProductServiceGrpc.ProductServiceImplBase {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    UserDataRepository userDataRepository;

    @Autowired
    ProcessingProductData processingProductData;
    Map<String, String> recentlyViewedDetails = new HashMap<>();
    private static final String INPUT_OUTPUT_TOPIC = "ProductData";

    private  KafkaStreams streams;

    private  KafkaStreams kafkaStreams;

    @Autowired
    CategoryRepository categoryRepository;

    HazelcastInstance hazelcastInstance = HazelcastClient.newHazelcastClient();

    ISet<String> distributedSet = hazelcastInstance.getSet("User Data");
    IMap<String,List<Object>> map = hazelcastInstance.getMap("Product Information");
    IMap<String, String> recentlyViewedData = hazelcastInstance.getMap("Recently Viewed Data");

    IMap<String,List<Map<String,String>>> limitedMap = hazelcastInstance.getMap("limitedMap");
    int maxSize = 6;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    ObjectMapper objectMapper = new ObjectMapper();
    List<com.osc.entity.Products> allproducts = hazelcastInstance.getList("AllProductData");

    Map<String,String> allCategories = hazelcastInstance.getMap("AllCategoriesData");
    Map<String,Map<String,com.osc.entity.Products>> allProductsMap = hazelcastInstance.getMap("AllProductsMap");
    //map convert
    public ProductService(){
    }

    //this getProductData() method will run every time when application gets started
    @EventListener(ApplicationReadyEvent.class)
    public void getProductData(){
        getAllProduct();
        updateViewCount();
        updateCartInfo();
    }

    public void getAllProduct(){
        allproducts = productRepository.findAll();
        List<com.osc.entity.Products> productsMap = productRepository.findAll();
        allProductsMap = productsMap.stream().collect(Collectors.groupingBy(com.osc.entity.Products::getCategoryId,
                                Collectors.toMap(com.osc.entity.Products:: getProductId, p -> p)));
        List<Categories> categoriesData = categoryRepository.findAll();
        allCategories = categoriesData.stream().collect(Collectors.toMap(Categories::getCategoryId, Categories::getCategoryName));
        Properties streamsProperties = new Properties();
        streamsProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-streams-appp-" + UUID.randomUUID().toString());
        streamsProperties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        streamsProperties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsProperties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.Integer().getClass().getName());
        streamsProperties.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class.getName());
        streamsProperties.put(StreamsConfig.STATE_DIR_CONFIG, "C:/Users/Gaurav.s/AppData/Local/Temp/kafka-streams/data" + streamsProperties.getProperty(StreamsConfig.APPLICATION_ID_CONFIG));

        StreamsBuilder builder = new StreamsBuilder();
        // Input and output topic
        KTable<String, Integer> kTable = builder.table(INPUT_OUTPUT_TOPIC, Materialized.as("ktable-topic-storee"));

        // Build the Kafka Streams topology
        Topology topology = builder.build();

        // Create a KafkaStreams instance
        streams = new KafkaStreams(topology, streamsProperties);

        // Set an uncaught exception handler
        streams.setUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("Error during KafkaStreams startup: " + throwable.getMessage());
            System.exit(1);
        });
        new Thread(streams::start).start();
        while (streams.state() != KafkaStreams.State.RUNNING) {
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }

        List<com.osc.entity.Products> products = productRepository.findAll();
        for(com.osc.entity.Products data : products){
            List<Object> productsList = new ArrayList<>();
            productsList.add(data.getCategoryId());
            productsList.add(data.getProdName());
            productsList.add(data.getProductDescription());
            productsList.add(data.getViewCount());
            productsList.add(data.getProdMarketPrice());

            map.put(data.getProductId(),productsList);
            produceMessage(INPUT_OUTPUT_TOPIC, data.getProductId(), data.getViewCount());
        }

        scheduler.scheduleAtFixedRate(this::updateViewCountData, 0, 30, TimeUnit.SECONDS);
    }
    public void updateCartInfo(){
        Properties streamsProperties = new Properties();
        streamsProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-streams-appp-" + UUID.randomUUID().toString());
        streamsProperties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        streamsProperties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsProperties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsProperties.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class.getName());
        streamsProperties.put(StreamsConfig.STATE_DIR_CONFIG, "C:/Users/Gaurav.s/AppData/Local/Temp/kafka-streams/data" + streamsProperties.getProperty(StreamsConfig.APPLICATION_ID_CONFIG));

        StreamsBuilder builder = new StreamsBuilder();
        // Input and output topic
        KTable<String, String> kTable = builder.table("CARTDATA", Materialized.as("ktable-topic-storeee"));
        // Build the Kafka Streams topology
        Topology topology = builder.build();

        // Create a KafkaStreams instance
        kafkaStreams = new KafkaStreams(topology, streamsProperties);

        // Set an uncaught exception handler
        kafkaStreams.setUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("Error during KafkaStreams startup: " + throwable.getMessage());
            System.exit(1);
        });
        new Thread(kafkaStreams::start).start();
        while (kafkaStreams.state() != KafkaStreams.State.RUNNING) {
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void updateViewCount(){
        Properties streamsProperties = new Properties();
        streamsProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-streams-appp-" + UUID.randomUUID().toString());
        streamsProperties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        streamsProperties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsProperties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.Integer().getClass().getName());
        streamsProperties.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class.getName());
        streamsProperties.put(StreamsConfig.STATE_DIR_CONFIG, "C:/Users/Gaurav.s/AppData/Local/Temp/kafka-streams/data" + streamsProperties.getProperty(StreamsConfig.APPLICATION_ID_CONFIG));

        StreamsBuilder builder = new StreamsBuilder();
        // Input and output topic
        KTable<String, Integer> kTable = builder.table(INPUT_OUTPUT_TOPIC, Materialized.as("ktable-topic-storee"));

        // Build the Kafka Streams topology
        Topology topology = builder.build();

        // Create a KafkaStreams instance
        streams = new KafkaStreams(topology, streamsProperties);

        // Set an uncaught exception handler
        streams.setUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("Error during KafkaStreams startup: " + throwable.getMessage());
            System.exit(1);
        });
        new Thread(streams::start).start();
        while (streams.state() != KafkaStreams.State.RUNNING) {
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void updateViewCountData(){
        ReadOnlyKeyValueStore<String, Integer> keyValueStore = streams.store(
                StoreQueryParameters.fromNameAndType("ktable-topic-storee", QueryableStoreTypes.keyValueStore())
        );

        keyValueStore.all().forEachRemaining(kv->{
            String key = kv.key;
            Integer value = kv.value;

            List<Object> updateData = map.get(key);

            if (updateData != null) {
                updateData.add(value);
                map.put(key, updateData);

                com.osc.entity.Products products = productRepository.findByProductId(key);
                products.setViewCount(value);
                productRepository.save(products);
            } else {
                System.out.println("UpdateData is null for key: " + key);
            }
        });
    }

    public void kafkaMessageProducer(String topic, String key, String value) {
        Properties producerProperties = new Properties();
        producerProperties.put("bootstrap.servers", "localhost:9092");
        producerProperties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProperties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProperties)) {
            producer.send(new ProducerRecord<>(topic, key, value));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void produceMessage(String topic, String key, Integer value) {
        Properties producerProperties = new Properties();
        producerProperties.put("bootstrap.servers", "localhost:9092");
        producerProperties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProperties.put("value.serializer", "org.apache.kafka.common.serialization.IntegerSerializer");

        try (KafkaProducer<String, Integer> producer = new KafkaProducer<>(producerProperties)) {
            producer.send(new ProducerRecord<>(topic, key, value));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void getProductData(ProductData request, StreamObserver<ProductDataResponse> responseObserver) {
            String userId = distributedSet.iterator().next();
            UserData userDetails  = userDataRepository.findRecentlyViewedDataByUserId(userId);

        if (userDetails != null && userDetails.getRecentlyViewedDetails() != null && !userDetails.getRecentlyViewedDetails().isEmpty() && !userDetails.getRecentlyViewedDetails().equals("null")) {
            responseObserver.onNext(processingProductData.existingUser(userId,userDetails,allproducts ));
            responseObserver.onCompleted();
        }else {
            String data = request.getRequest();
            UserData newUser = new UserData();
            newUser.setUserId(userId);
            userDataRepository.save(newUser);
            ProductDataResponse.Builder productDataResponse = ProductDataResponse.newBuilder();
            if (data.equals("All")) {
                UserDashBoardData.Builder aResponse = UserDashBoardData.newBuilder();
                aResponse.setTYPE("Featured Products");
                List<com.osc.entity.Products> productList = allProductsMap.values().stream()
                        .flatMap(productMap -> productMap.values().stream())
                        .sorted(Comparator.comparingInt(com.osc.entity.Products::getViewCount).reversed())
                        .collect(Collectors.toList());
                for (com.osc.entity.Products value : productList) {
                    com.osc.product.Products products = Products.newBuilder()
                            .setProductId(value.getProductId())
                            .setCategoryId(value.getCategoryId())
                            .setProdName(value.getProdName())
                            .setProdMarketPrice(value.getProdMarketPrice())
                            .setProductDescription(value.getProductDescription())
                            .setViewCount(value.getViewCount()).build();
                    aResponse.addProducts(products);
                }
                ListOfUserData.Builder itemsResponse = ListOfUserData.newBuilder();
                itemsResponse.addUserDashBoardData(aResponse);
                UserDashBoardData.Builder categoryBuilder = UserDashBoardData.newBuilder();
                categoryBuilder.setTYPE("Categories");
                for (Map.Entry<String,String> info : allCategories.entrySet()) {
                    com.osc.product.Categories categories = com.osc.product.Categories.newBuilder()
                            .setCategoryId(info.getKey())
                            .setCategoryName(info.getValue()).build();

                    categoryBuilder.addCategories(categories);
                }
                itemsResponse.addUserDashBoardData(categoryBuilder);
                productDataResponse.setListOfUserData(itemsResponse);
                productDataResponse.setValue(true);
            }
            ProductDataResponse response = productDataResponse.build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    public void updatingViewCountOfProduct(String prodId) {
        ReadOnlyKeyValueStore<String, Integer> keyValueStore = streams.store(
                StoreQueryParameters.fromNameAndType("ktable-topic-storee", QueryableStoreTypes.keyValueStore())
        );
        Integer value = keyValueStore.get(prodId);

        if (value != null) {
            // Produce a message to the input topic
            produceMessage(INPUT_OUTPUT_TOPIC, prodId,value+1 );
        }
    }

    @Override
    public void socketQuery(SocketRequest request, StreamObserver<SocketResponse> responseObserver) {
        try {
            String MT = request.getMT();
            String ProdId = request.getProdId();
            String CatId = request.getCatId();
            String filter = request.getFilter();
            String userId = request.getUserId() ;

            mtResponse(responseObserver, MT, ProdId, CatId, filter, userId);
        }
        catch (Exception e){
            System.out.println(e);
        }
    }

    public void mtResponse(StreamObserver<SocketResponse> responseObserver, String MT, String ProdId, String CatId, String filter, String userId) throws JsonProcessingException {
        switch (MT){
            case "2":
                try {
                    SocketResponse socketResponse = SocketResponse.newBuilder().setResponse(handleMT2(ProdId,CatId,userId,MT)).build();
                    responseObserver.onNext(socketResponse);
                    responseObserver.onCompleted();
                }catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            case "3":
                switch (filter) {
                    case "LH":
                        try {
                        SocketResponse socket_Response = SocketResponse.newBuilder().setResponse(handleFilterLH(MT,CatId)).build();
                        responseObserver.onNext(socket_Response);
                        responseObserver.onCompleted();
                        }
                        catch(Exception e){
                            throw new RuntimeException(e);
                        }
                        break;
                    case "P":
                        try{
                        SocketResponse socket = SocketResponse.newBuilder().setResponse(handleFilterP(MT,CatId)).build();
                        responseObserver.onNext(socket);
                        responseObserver.onCompleted();
                        }catch(Exception e){
                            throw new RuntimeException(e);
                        }
                        break;
                    case "HL":
                        try {
                        SocketResponse newSocketResponse = SocketResponse.newBuilder().setResponse(handleFilterHL(MT, CatId)).build();
                        responseObserver.onNext(newSocketResponse);
                        responseObserver.onCompleted();
                        }catch(Exception e){
                            throw new RuntimeException(e);
                        }
                        break;
                    case "NF":
                        try {
                        SocketResponse responseOfSocket = SocketResponse.newBuilder().setResponse(handleFilterNF(MT,CatId)).build();
                        responseObserver.onNext(responseOfSocket);
                        responseObserver.onCompleted();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    default:
                        break;
                }
                break;
            case "6":
                ReadOnlyKeyValueStore<String, String> ketValueData = kafkaStreams.store(
                        StoreQueryParameters.fromNameAndType("ktable-topic-storeee", QueryableStoreTypes.keyValueStore())
                );
                String listOfValues = ketValueData.get(userId);
                List<Map<String,String>> cartDetails = objectMapper.readValue(listOfValues,new TypeReference<>(){});
                try {
                SocketResponse socket = SocketResponse.newBuilder().setResponse(handleMT6(cartDetails,MT)).build();
                responseObserver.onNext(socket);
                responseObserver.onCompleted();
                }catch(Exception e){
                    throw new RuntimeException(e);
                }
                break;
            case "8":
                try {
                ReadOnlyKeyValueStore<String, String> keyValueStoree = kafkaStreams.store(
                        StoreQueryParameters.fromNameAndType("ktable-topic-storeee", QueryableStoreTypes.keyValueStore())
                );
                String value = keyValueStoree.get(userId);

                    handleMT8(MT,value,ProdId,userId);
                SocketResponse socket_Response = SocketResponse.newBuilder().setResponse("").build();
                responseObserver.onNext(socket_Response);
                responseObserver.onCompleted();
                }catch(Exception e){
                    throw new RuntimeException(e);
                }
                break;
            case "9":
                try {
                ReadOnlyKeyValueStore<String, String> keyValueStore = kafkaStreams.store(
                        StoreQueryParameters.fromNameAndType("ktable-topic-storeee", QueryableStoreTypes.keyValueStore())
                );
                String values = keyValueStore.get(userId);
                    handleMT9(values,ProdId,userId);
                SocketResponse newSocketResponse = SocketResponse.newBuilder().setResponse("").build();
                responseObserver.onNext(newSocketResponse);
                responseObserver.onCompleted();
                }catch(Exception e){
                    throw new RuntimeException(e);
                }
                break;
            case "10":
                ReadOnlyKeyValueStore<String, String> keyValueStorree = kafkaStreams.store(
                        StoreQueryParameters.fromNameAndType("ktable-topic-storeee", QueryableStoreTypes.keyValueStore())
                );
                String productData = keyValueStorree.get(userId);
                try {
                    handleMT10(productData,ProdId,userId);
                }catch(Exception e){
                    throw new RuntimeException(e);
                }
                break;
            default:
                break;
        }
    }

    public void dataUpdationOnLogOut() throws JsonProcessingException {
        ReadOnlyKeyValueStore<String, String> keyValueStore = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType("ktable-topic-storeee", QueryableStoreTypes.keyValueStore())
        );
        String userId = distributedSet.iterator().next();
        if(userId!=null) {
            String value = keyValueStore.get(userId);
            List<Map<String, String>> mapvalues = limitedMap.get(userId);
            String recentViewedProduct = objectMapper.writeValueAsString(mapvalues);
            UserData userData = userDataRepository.findByUserId(userId);
            userData.setUserId(userId);
            userData.setCartDetails(value);
            userData.setRecentlyViewedDetails(recentViewedProduct);
            userDataRepository.save(userData);
        }
    }

    public String handleMT2(String ProdId, String CatId, String userId, String MT){
        try {
            String loggedInUser = distributedSet.iterator().next();
            List<Map<String, String>> getData = limitedMap.get(loggedInUser);
            if (getData == null) {
                getData = new ArrayList<>();
            }
            if (getData.size() >= maxSize) {
                // Remove the oldest entry (1st map)
                getData.remove(0);
            }
            if (!recentlyViewedDetails.containsValue(ProdId)) {
                // ProdId not present, add a new entry
                recentlyViewedDetails.put("prodId", ProdId);
                recentlyViewedDetails.put("Counts", String.valueOf(getData.size() + 1));
                getData.add(recentlyViewedDetails);
            }


            for (int i = 0; i < getData.size(); i++) {
                getData.get(i).put("Counts", String.valueOf(i + 1));
            }
            limitedMap.put(loggedInUser, getData);
            String vaal = null;
            vaal = objectMapper.writeValueAsString(limitedMap);
            recentlyViewedData.put("Recently Viewed Products", vaal);
            Map<String, com.osc.entity.Products> allProductData = allProductsMap.get(CatId);
            com.osc.entity.Products product = allProductData.get(ProdId);;
            updatingViewCountOfProduct(ProdId);
            List<com.osc.entity.Products> similarProducts = allProductData.values().stream().collect(Collectors.toList());
            List<Map<String, Object>> similarProductsList = new ArrayList<>();

            Set<String> addedProductIds = new HashSet<>(); // To track added productIds

            for (com.osc.entity.Products similarProduct : similarProducts) {
                String productId = similarProduct.getProductId();

                if (!addedProductIds.contains(productId) && !ProdId.equals(productId)) {
                    // Product not yet added, add it to the list
                    Map<String, Object> similarProductMap = new HashMap<>();
                    similarProductMap.put("productId", productId);
                    similarProductMap.put("ProdName", similarProduct.getProdName());
                    similarProductMap.put("prodMarketPrice", similarProduct.getProdMarketPrice());
                    similarProductMap.put("categoryId", similarProduct.getCategoryId());
                    similarProductsList.add(similarProductMap);

                    // Add the productId to the set
                    addedProductIds.add(productId);

                    // Check if 6 unique products have been added, if yes, break the loop
                    if (similarProductsList.size() >= 6) {
                        break;
                    }
                }
            }

            similarProductsList.removeIf(productMap -> ProdId.equals(productMap.get("productId")));

            Map<String, Object> mergedData = new HashMap<>();
            mergedData.put("catId", CatId);
            mergedData.put("prodId", ProdId);
            mergedData.put("MT", MT);
            mergedData.put("prodName", product.getProdName());
            mergedData.put("prodMarketPrice", product.getProdMarketPrice());
            mergedData.put("prodDesc", product.getProductDescription());
            mergedData.put("similarProducts", similarProductsList);

            String jsonResponse = objectMapper.writeValueAsString(mergedData);
            return jsonResponse;
        }catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
    public String handleFilterLH(String MT, String CatId){
        try {
            Map<String, com.osc.entity.Products> allProductData = allProductsMap.get(CatId);
            List<com.osc.entity.Products> products =allProductData.values().stream().collect(Collectors.toList());
            List<com.osc.entity.Products> sortedAsc = products.stream()
                    .sorted(Comparator.comparing(com.osc.entity.Products::getProdMarketPrice))
                    .collect(Collectors.toList());

            List<Map<String, Object>> similarProductsLisst = new ArrayList<>();

            for (com.osc.entity.Products similarProduct : sortedAsc) {
                Map<String, Object> similarProductMap = new HashMap<>();
                similarProductMap.put("productId", similarProduct.getProductId());
                similarProductMap.put("ProdName", similarProduct.getProdName());
                similarProductMap.put("prodMarketPrice", similarProduct.getProdMarketPrice());
                similarProductsLisst.add(similarProductMap);
            }
            Map<String, Object> allCombinedData = new HashMap<>();
            allCombinedData.put("MT", MT);
            allCombinedData.put("catId", CatId);
            allCombinedData.put("products", similarProductsLisst);

            String json_Response = objectMapper.writeValueAsString(allCombinedData);
            return json_Response;
        }
        catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public String handleFilterP(String MT,String CatId){
        try{
            List<com.osc.entity.Products> productByCategoryId = productRepository.findByCategoryId(CatId);
            List<Map<String, Object>> similarProductsListt = new ArrayList<>();

            for (com.osc.entity.Products similarProduct : productByCategoryId) {
                Map<String, Object> similarProductMap = new HashMap<>();
                similarProductMap.put("productId", similarProduct.getProductId());
                similarProductMap.put("ProdName", similarProduct.getProdName());
                similarProductMap.put("prodMarketPrice", similarProduct.getProdMarketPrice());
                similarProductsListt.add(similarProductMap);
            }
            Map<String, Object> merged_Data = new HashMap<>();
            merged_Data.put("MT", MT);
            merged_Data.put("catId", CatId);
            merged_Data.put("products", similarProductsListt);

            String json = objectMapper.writeValueAsString(merged_Data);
            return json;
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
    public String handleFilterHL(String MT,String CatId){
        try {
            Map<String, com.osc.entity.Products> allProductData = allProductsMap.get(CatId);
            List<com.osc.entity.Products> productByPrice = allProductData.values().stream().collect(Collectors.toList());
            List<com.osc.entity.Products> sortedDesc = productByPrice.stream()
                    .sorted(Comparator.comparing(com.osc.entity.Products::getProdMarketPrice).reversed())
                    .collect(Collectors.toList());
            List<Map<String, Object>> productsList = new ArrayList<>();

            for (com.osc.entity.Products similarProduct : sortedDesc) {
                Map<String, Object> similarProductMap = new HashMap<>();
                similarProductMap.put("productId", similarProduct.getProductId());
                similarProductMap.put("ProdName", similarProduct.getProdName());
                similarProductMap.put("prodMarketPrice", similarProduct.getProdMarketPrice());
                productsList.add(similarProductMap);
            }
            Map<String, Object> newData = new HashMap<>();
            newData.put("MT", MT);
            newData.put("catId", CatId);
            newData.put("products", productsList);

            String responseJson = objectMapper.writeValueAsString(newData);
            return responseJson;
        }catch(Exception e){
            throw new RuntimeException(e);
        }

    }
    public String handleFilterNF(String MT,String CatId) {
        try {
            List<com.osc.entity.Products> productByCategoryId = productRepository.findByCategoryId(CatId);
            List<Map<String, Object>> similarProductsListt = new ArrayList<>();
            productByCategoryId = productRepository.findByCategoryId(CatId);
            similarProductsListt = new ArrayList<>();

            for (com.osc.entity.Products similarProduct : productByCategoryId) {
                Map<String, Object> similarProductMap = new HashMap<>();
                similarProductMap.put("productId", similarProduct.getProductId());
                similarProductMap.put("ProdName", similarProduct.getProdName());
                similarProductMap.put("prodMarketPrice", similarProduct.getProdMarketPrice());
                similarProductsListt.add(similarProductMap);
            }
            Map<String, Object> new_Data = new HashMap<>();
            new_Data.put("MT", MT);
            new_Data.put("catId", CatId);
            new_Data.put("products", similarProductsListt);
            String newJson = objectMapper.writeValueAsString(new_Data);
            return newJson;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String handleMT6(List<Map<String,String>> cartDetails,String MT){
        try {
            double price = 0;

            for (Map<String, String> val : cartDetails) {
                String productId = val.get("prodId");
                com.osc.entity.Products prp = productRepository.findByProductId(productId);

                val.put("prodName", prp.getProdName());
                val.put("price", String.valueOf(prp.getProdMarketPrice()));

                double quant = Integer.valueOf(val.get("cartQty")) * prp.getProdMarketPrice();
                price += quant;
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("MT", MT);
            responseData.put("cartProducts", cartDetails);
            responseData.put("productsCartCount", String.valueOf(cartDetails.size()));
            responseData.put("totalPrice", String.valueOf(price));
            String json = objectMapper.writeValueAsString(responseData);
            return json;
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public void handleMT8(String MT,String value,String ProdId,String userId){
        try {
            List<Map<String, String>> cartDetailsList = new ArrayList<>();
            if (value != null && value.matches("\\p{Print}+")) {
                try {
                    cartDetailsList = objectMapper.readValue(value, new TypeReference<List<Map<String, String>>>() {
                    });
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                // Iterate through the cartList to find and update the quantity
                for (Map<String, String> item : cartDetailsList) {
                    String prodIdd = item.get("prodId");
                    if (prodIdd != null && prodIdd.equals(ProdId)) {
                        // Found the item with the target Prod_Id, increase the quantity
                        int currentQuantity = Integer.parseInt(item.get("cartQty"));
                        if(currentQuantity > 0){
                            int newQuantity = --currentQuantity;
                            item.put("cartQty", String.valueOf(newQuantity));
                        }
                    }
                }

                // Convert the updated cartList to a string
                String cart = null;
                try {
                    cart = objectMapper.writeValueAsString(cartDetailsList);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                // Produce a message to the input topic
                kafkaMessageProducer("CARTDATA", userId, cart);
            }
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public void handleMT9(String values,String ProdId,String userId){
        try {
            int quantity = 1;
            String storeData = null;
            List<Map<String, String>> cartList = new ArrayList<>();
            if (values != null && values.matches("\\p{Print}+")) {
                try {
                    cartList = objectMapper.readValue(values, new TypeReference<List<Map<String, String>>>() {});
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                boolean found = false;
                // Iterate through the cartList to find and update the quantity
                for (Map<String, String> item : cartList) {
                    String prodIdd = item.get("prodId");
                    if (prodIdd != null && prodIdd.equals(ProdId)) {
                        // Found the item with the target Prod_Id, increase the quantity
                        int currentQuantity = Integer.parseInt(item.get("cartQty"));
                        int newQuantity = currentQuantity + 1;
                        item.put("cartQty", String.valueOf(newQuantity));
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    // If Prod_Id is not found in the cartList, add a new entry
                    Map<String, String> datas = new HashMap<>();
                    datas.put("prodId", ProdId);
                    datas.put("cartQty", String.valueOf(quantity));
                    cartList.add(datas);
                }

                try {
                    storeData = objectMapper.writeValueAsString(cartList);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                // Produce a message to the input topic
                kafkaMessageProducer("CARTDATA", userId, storeData);
            } else {
                // If no existing data, create a new entry
                Map<String, String> datas = new HashMap<>();
                datas.put("prodId", ProdId);
                datas.put("cartQty", String.valueOf(quantity));
                cartList.add(datas);
                try {
                    storeData = objectMapper.writeValueAsString(cartList);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                kafkaMessageProducer("CARTDATA", userId, storeData);
            }
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public void handleMT10(String productData,String ProdId,String userId){
        try {
            List<Map<String, String>> listOfCart = new ArrayList<>();
            if (productData != null && productData.matches("\\p{Print}+")) {
                System.out.println(productData);
                try {
                    listOfCart = objectMapper.readValue(productData, new TypeReference<List<Map<String, String>>>() {
                    });
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                // Iterate through the cartList to find and update the quantity
                Iterator<Map<String, String>> iterator = listOfCart.iterator();
                while (iterator.hasNext()) {
                    Map<String, String> item = iterator.next();
                    String prodId = item.get("prodId");
                    if (prodId != null && prodId.equals(ProdId)) {
                        iterator.remove();
                        break;
                    }
                }
                String storeProductData = null;
                // Convert the updated cartList to a string
                try {
                    storeProductData = objectMapper.writeValueAsString(listOfCart);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                // Produce a message to the input topic
                kafkaMessageProducer("CARTDATA", userId, storeProductData);
            }
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}

