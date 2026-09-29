package com.rbdip.bookstore.order;

import com.rbdip.bookstore.product.Product;

record OrderLine(Product product, int quantity) {
}