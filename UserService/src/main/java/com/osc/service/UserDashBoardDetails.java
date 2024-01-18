package com.osc.service;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.osc.dto.Cart;
import com.osc.dto.ExistingUserDashboardData;
import com.osc.dto.NewUserDashboardData;
import com.osc.dto.Products;
import com.osc.product.ListOfUserData;
import com.osc.product.UserDashBoardData;

import java.util.ArrayList;
import java.util.List;

public class UserDashBoardDetails {
    public static void newUserDashBorard(ListOfUserData grpcResponse, List<NewUserDashboardData> userDashboardData) {
        for (UserDashBoardData grpcA : grpcResponse.getUserDashBoardDataList()) {
            NewUserDashboardData newUserDashboardData = new NewUserDashboardData();
            newUserDashboardData.setTYPE(grpcA.getTYPE());

            List<com.osc.dto.Products> dtoProductList = new ArrayList<>();
            for (com.osc.product.Products grpcProduct : grpcA.getProductsList()) {
                com.osc.dto.Products dtoProduct = new com.osc.dto.Products();
                dtoProduct.setProductId(grpcProduct.getProductId());
                dtoProduct.setCategoryId(grpcProduct.getCategoryId());
                dtoProduct.setProdName(grpcProduct.getProdName());
                dtoProduct.setProdMarketPrice(grpcProduct.getProdMarketPrice());
                dtoProduct.setProductDescription(grpcProduct.getProductDescription());
                dtoProduct.setViewCount(grpcProduct.getViewCount());
                dtoProductList.add(dtoProduct);
            }
            newUserDashboardData.setFeaturedProducts(dtoProductList);

            List<com.osc.dto.Categories> dtoCategoryList = new ArrayList<>();
            for (com.osc.product.Categories grpcCategory : grpcA.getCategoriesList()) {
                com.osc.dto.Categories dtoCategory = new com.osc.dto.Categories();
                dtoCategory.setCategoryId(grpcCategory.getCategoryId());
                dtoCategory.setCategoryName(grpcCategory.getCategoryName());
                dtoCategoryList.add(dtoCategory);
            }
            newUserDashboardData.setCategories(dtoCategoryList);
            userDashboardData.add(newUserDashboardData);
        }
    }

    public static void existingUserDashboard(String userId, ListOfUserData grpcResponse, List<ExistingUserDashboardData> userDashboardData) {
        for (UserDashBoardData grpcA : grpcResponse.getUserDashBoardDataList()) {
            ExistingUserDashboardData existingUserDashboardData = new ExistingUserDashboardData();
            existingUserDashboardData.setTYPE(grpcA.getTYPE());
            List<com.osc.dto.Products> dtoProductList = new ArrayList<>();
            for (com.osc.product.Products grpcProduct : grpcA.getProductsList()) {
                com.osc.dto.Products dtoProduct = new com.osc.dto.Products();
                dtoProduct.setProductId(grpcProduct.getProductId());
                dtoProduct.setCategoryId(grpcProduct.getCategoryId());
                dtoProduct.setProdName(grpcProduct.getProdName());
                dtoProduct.setProdMarketPrice(grpcProduct.getProdMarketPrice());
                dtoProduct.setProductDescription(grpcProduct.getProductDescription());
                dtoProduct.setViewCount(grpcProduct.getViewCount());
                dtoProductList.add(dtoProduct);
            }
            existingUserDashboardData.setRecentlyViewedProducts(dtoProductList);

            List<com.osc.dto.Categories> dtoCategoryList = new ArrayList<>();
            for (com.osc.product.Categories grpcCategory : grpcA.getCategoriesList()) {
                com.osc.dto.Categories dtoCategory = new com.osc.dto.Categories();
                dtoCategory.setCategoryId(grpcCategory.getCategoryId());
                dtoCategory.setCategoryName(grpcCategory.getCategoryName());
                dtoCategoryList.add(dtoCategory);
            }
            existingUserDashboardData.setCategories(dtoCategoryList);

            Gson gson = new Gson();
            java.lang.reflect.Type listType = new TypeToken<List<Products>>() {
            }.getType();
            List<Products> productList = gson.fromJson(grpcA.getSimilarProducts(), listType);
            existingUserDashboardData.setSimilarProducts(productList);

            String cartJson = grpcA.getCart();
            Cart cart = new Cart();
            if (cartJson != null) {
                Cart cartResponse = gson.fromJson(cartJson, Cart.class);

                // Checking if cartResponse is not null before accessing its methods
                if (cartResponse != null) {
                    cart.setUserId(userId);
                    cart.setProductsCartCount(cartResponse.getProductsCartCount());
                    cart.setCartProducts(cartResponse.getCartProducts());
                    cart.setTotalPrice(cartResponse.getTotalPrice());
                } else {
                    System.out.println("The deserialized Cart object is null.");
                }
            } else {
                System.out.println("The JSON string is null.");
            }
            existingUserDashboardData.setCART(cart);
            userDashboardData.add(existingUserDashboardData);
        }
    }
}
