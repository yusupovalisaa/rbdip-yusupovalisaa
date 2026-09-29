package com.rbdip.bookstore.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createProduct(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        BigDecimal price = new BigDecimal(body.get("price").toString());
        String description = (String) body.get("description");
        Product saved = productRepository.save(new Product(name, price, description));
        return Map.of("id", saved.getId(), "name", saved.getName());
    }

    @GetMapping("/products")
    public List<Map<String, Object>> listProducts() {
        return productRepository.findAll().stream()
                .map(p -> Map.<String, Object>of(
                        "id", p.getId(),
                        "name", p.getName(),
                        "price", p.getPrice().toString()))
                .toList();
    }
}
