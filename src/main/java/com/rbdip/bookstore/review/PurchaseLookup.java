package com.rbdip.bookstore.review;

public interface PurchaseLookup {

    boolean hasPurchased(Long productId, String customerName);
}
