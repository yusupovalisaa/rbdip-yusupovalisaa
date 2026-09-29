package com.rbdip.bookstore.review;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/products/{productId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addReview(@PathVariable Long productId, @RequestBody Map<String, Object> body) {
        String authorName = (String) body.get("authorName");
        Integer rating = (Integer) body.get("rating");
        String comment = (String) body.get("comment");
        Review review = reviewService.addReview(productId, authorName, rating, comment);
        return Map.of("id", review.getId());
    }

    @GetMapping("/products/{productId}/reviews")
    public List<Map<String, Object>> listReviews(@PathVariable Long productId) {
        return reviewService.listReviews(productId).stream()
                .map(r -> Map.<String, Object>of(
                        "authorName", r.getAuthorName(),
                        "rating", r.getRating(),
                        "comment", r.getComment() == null ? "" : r.getComment()))
                .toList();
    }
}
