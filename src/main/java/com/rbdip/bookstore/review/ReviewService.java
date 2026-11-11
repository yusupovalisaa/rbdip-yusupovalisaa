package com.rbdip.bookstore.review;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final PurchaseLookup purchaseLookup;

    public ReviewService(ReviewRepository reviewRepository, PurchaseLookup purchaseLookup) {
        this.reviewRepository = reviewRepository;
        this.purchaseLookup = purchaseLookup;
    }

    public Review addReview(Long productId, String authorName, Integer rating, String comment) {
        boolean verifiedPurchase = authorName != null && purchaseLookup.hasPurchased(productId, authorName);
        Review review = new Review(productId, authorName == null ? "anonymous" : authorName, rating, comment);
        Review savedReview = reviewRepository.save(review);
        LOGGER.debug("Review {} matches a purchase: {}", savedReview.getId(), verifiedPurchase);
        return savedReview;
    }

    public List<Review> listReviews(Long productId) {
        return reviewRepository.findByProductId(productId);
    }
}
