/*
package com.osc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.collection.ISet;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.osc.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
public class WebSocketQueryHandler {
    ObjectMapper objectMapper = new ObjectMapper();

    HazelcastInstance hazelcastInstance = HazelcastClient.newHazelcastClient();

    IMap<String,List<Map<String,String>>> limitedMap = hazelcastInstance.getMap("limitedMap");

    IMap<String, String> recentlyViewedData = hazelcastInstance.getMap("Recently Viewed Data");


    ProductRepository productRepository;

    Map<String, String> recentlyViewedDetails = new HashMap<>();

    int maxSize = 6;

    ISet<String> distributedSet = hazelcastInstance.getSet("User Data");

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
            com.osc.entity.Products product = productRepository.findByCategoryIdAndProductId(CatId, ProdId);
            productService.updatingViewCountOfProduct(ProdId);
            List<com.osc.entity.Products> similarProducts = productRepository.findByCategoryId(CatId);
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
            List<com.osc.entity.Products> products = productRepository.findByPrice(CatId);
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
            List<com.osc.entity.Products> productByPrice = productRepository.findByPrice(CatId);
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
                productService.kafkaMessageProducer("CARTDATA", userId, cart);
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
                productService.kafkaMessageProducer("CARTDATA", userId, storeData);
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
                productService.kafkaMessageProducer("CARTDATA", userId, storeData);
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
                productService.kafkaMessageProducer("CARTDATA", userId, storeProductData);
            }
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}
*/
