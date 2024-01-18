
package com.osc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import com.osc.entity.Categories;
import com.osc.entity.UserData;
import com.osc.product.ListOfUserData;
import com.osc.product.ProductDataResponse;
import com.osc.product.Products;
import com.osc.product.UserDashBoardData;
import com.osc.repository.CategoryRepository;
import com.osc.repository.ProductRepository;
import com.osc.repository.UserDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProcessingProductData {
    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    UserDataRepository userDataRepository;

    @Autowired
    ProductRepository productRepository;
    public ProductDataResponse existingUser(String userId, UserData userDetails,List<com.osc.entity.Products> allproducts) {
        try {
            //Parse recently viewed data
            List<Map<String, String>> recentlyViewedData = parseRecentlyViewedData(userDetails);

            //Build Recently Viewed Products response
            UserDashBoardData.Builder recentlyViewedBuilder = buildRecentlyViewedResponse(recentlyViewedData,allproducts);

            //Build Categories response
            UserDashBoardData.Builder categoryBuilder = buildCategoriesResponse();

            //Generate Similar Products response
            UserDashBoardData.Builder similarProductsBuilder = buildSimilarProductsResponse(recentlyViewedBuilder.getProductsList(),allproducts);

            //Generate Cart response
            UserDashBoardData.Builder cartBuilder = buildCartResponse(userId,allproducts);

            //Combine all responses into a single response
            ListOfUserData.Builder itemsResponse = combineResponses(recentlyViewedBuilder, categoryBuilder, cartBuilder, similarProductsBuilder);

            //Build final ProductDataResponse
            ProductDataResponse.Builder productDataResponse = buildProductDataResponse(itemsResponse);

            //Set additional values for the response
            productDataResponse.setValue(false);

            //Return the final response
            return productDataResponse.build();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // Helper method to parse recently viewed data
    private List<Map<String, String>> parseRecentlyViewedData(UserData userDetails) {
        try {
            return objectMapper.readValue(
                    userDetails.getRecentlyViewedDetails(),
                    new TypeReference<List<Map<String, String>>>() {
                    }
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    // Helper method to build Recently Viewed Products response
    private UserDashBoardData.Builder buildRecentlyViewedResponse(List<Map<String, String>> recentlyViewedData,List<com.osc.entity.Products> allproducts) {
        UserDashBoardData.Builder recentlyViewedBuilder = UserDashBoardData.newBuilder();
        recentlyViewedBuilder.setTYPE("Recently Viewed Products");

        for (Map<String, String> val : recentlyViewedData) {
            String productId = val.get("prodId");
            System.out.println(productId);
            Optional<com.osc.entity.Products> productOptional = allproducts.stream()
                    .filter(product -> productId.equals(product.getProductId()))
                    .findFirst();
            com.osc.entity.Products prp = productOptional.get();
            val.put("prodName", prp.getProdName());
            val.put("categoryId", prp.getCategoryId());
            val.put("prodMarketPrice", String.valueOf(prp.getProdMarketPrice()));
            val.put("productDescription", prp.getProductDescription());
        }

        for (Map<String, String> val : recentlyViewedData) {
            com.osc.product.Products.Builder productBuilder = com.osc.product.Products.newBuilder();

            productBuilder.setProductId(val.get("prodId"));
            productBuilder.setCategoryId(val.get("categoryId"));
            productBuilder.setProdName(val.get("prodName"));
            productBuilder.setProdMarketPrice(Float.parseFloat(val.get("prodMarketPrice")));
            productBuilder.setProductDescription(val.get("productDescription"));
            productBuilder.setViewCount(Integer.parseInt(val.get("Counts")));

            recentlyViewedBuilder.addProducts(productBuilder.build());
        }

        return recentlyViewedBuilder;
    }

    // Helper method to build Categories response
    private UserDashBoardData.Builder buildCategoriesResponse() {
        UserDashBoardData.Builder categoryBuilder = UserDashBoardData.newBuilder();
        categoryBuilder.setTYPE("Categories");

        List<Categories> categoriesList = categoryRepository.findAll();

        for (Categories info : categoriesList) {
            com.osc.product.Categories categories = com.osc.product.Categories.newBuilder()
                    .setCategoryId(info.getCategoryId())
                    .setCategoryName(info.getCategoryName()).build();

            categoryBuilder.addCategories(categories);
        }

        return categoryBuilder;
    }

    // Helper method to build Similar Products response
    private UserDashBoardData.Builder buildSimilarProductsResponse(List<Products> productsList, List<com.osc.entity.Products> allproducts) {
        UserDashBoardData.Builder similarProductsBuilder = UserDashBoardData.newBuilder();
        similarProductsBuilder.setTYPE("Similar Products");

        List<com.osc.entity.Products> updatedList = new ArrayList<>();
        Set<String> productIds = productsList.stream()
                .map(Products::getProductId)
                .collect(Collectors.toSet());

        for (Products products : productsList) {
            int limit = 1;
            int offset = 0;
            do {
                com.osc.entity.Products dataUser = productRepository.findTopByCategoryId(products.getCategoryId(), limit, offset);

                // Checking if the product ID is equal to any existing product ID
                boolean productIdNotEqual = productIds.stream()
                        .anyMatch(productId -> productId.equals(dataUser.getProductId()));

                // Checking if the product is already present in updatedList
                boolean anyProductAlreadyPresent = updatedList.stream()
                        .anyMatch(product -> product.getProductId().equals(dataUser.getProductId()));

                if (anyProductAlreadyPresent || productIdNotEqual) {
                    // Product found in either updatedList or productIds
                    offset++;
                } else {
                    // Product is unique, add it to updatedList
                    updatedList.add(dataUser);
                    offset = 1;
                    break;
                }
            } while (offset < 15);
        }

        while (updatedList.size() < 6) {
            com.osc.entity.Products productDetails = updatedList.get(updatedList.size() - 1);
            int limit = 1;
            int offset = 0;
            do {
                com.osc.entity.Products dataUser = productRepository.findTopByCategoryId(productDetails.getCategoryId(), limit, offset);

                // Checking if the product ID is equal to any existing product ID
                boolean productIdNotEqual = productIds.stream()
                        .anyMatch(productId -> productId.equals(dataUser.getProductId()));

                // Checking if the product is already present in updatedList
                boolean anyProductAlreadyPresent = updatedList.stream()
                        .anyMatch(product -> product.getProductId().equals(dataUser.getProductId()));

                if (anyProductAlreadyPresent || productIdNotEqual) {
                    // Product found in either updatedList or productIds
                    offset++;
                } else {
                    // Product is unique, add it to updatedList
                    updatedList.add(dataUser);
                    offset = 1;
                    break;
                }
            } while (offset < 15);
        }

        String upDatedProductList = null;
        try {
            upDatedProductList = objectMapper.writeValueAsString(updatedList);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        similarProductsBuilder.setSimilarProducts(upDatedProductList);

        return similarProductsBuilder;
    }

    // Helper method to build Cart response
    private UserDashBoardData.Builder buildCartResponse(String userId,List<com.osc.entity.Products> allproducts) {
        UserDashBoardData.Builder cartBuilder = UserDashBoardData.newBuilder();
        cartBuilder.setTYPE("Cart");

        UserData data = userDataRepository.findByUserId(userId);

        List<Map<String, String>> list = new ArrayList<>();
        double price = 0;

        if (data.getCartDetails() != null) {
            try {
                list = objectMapper.readValue(data.getCartDetails(), new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            for (Map<String, String> val : list) {
                String productId = val.get("prodId");
                Optional<com.osc.entity.Products> productOptional = allproducts.stream()
                        .filter(product -> productId.equals(product.getProductId()))
                        .findFirst();
                com.osc.entity.Products prp = productOptional.get();

                val.put("prodName", prp.getProdName());
                val.put("price", String.valueOf(prp.getProdMarketPrice()));

                double quant = Integer.valueOf(val.get("cartQty")) * prp.getProdMarketPrice();
                price += quant;
            }
        }

        Map<String, Object> responseDData = new HashMap<>();
        responseDData.put("cartProducts", list);
        responseDData.put("ProductsCartCount", String.valueOf(list.size()));
        responseDData.put("totalPrice", String.valueOf(price));
        String jsonResposnse = null;

        try {
            jsonResposnse = objectMapper.writeValueAsString(responseDData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        cartBuilder.setCart(jsonResposnse);

        return cartBuilder;
    }

    // Helper method to combine all responses into a single response
    private ListOfUserData.Builder combineResponses(
            UserDashBoardData.Builder recentlyViewedBuilder,
            UserDashBoardData.Builder categoryBuilder,
            UserDashBoardData.Builder cartBuilder,
            UserDashBoardData.Builder similarProductsBuilder
    ) {
        ListOfUserData.Builder itemsResponse = ListOfUserData.newBuilder();
        itemsResponse.addUserDashBoardData(recentlyViewedBuilder);
        itemsResponse.addUserDashBoardData(categoryBuilder);
        itemsResponse.addUserDashBoardData(cartBuilder);
        itemsResponse.addUserDashBoardData(similarProductsBuilder);

        return itemsResponse;
    }

    // Helper method to build final ProductDataResponse
    private ProductDataResponse.Builder buildProductDataResponse(ListOfUserData.Builder itemsResponse) {
        ProductDataResponse.Builder productDataResponse = ProductDataResponse.newBuilder();
        productDataResponse.setListOfUserData(itemsResponse);

        return productDataResponse;
    }

}