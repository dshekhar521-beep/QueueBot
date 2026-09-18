package com.ticketintelligence.ticket_intelligence.service;

import com.ticketintelligence.ticket_intelligence.entity.Product;
import com.ticketintelligence.ticket_intelligence.entity.Seller;
import com.ticketintelligence.ticket_intelligence.entity.User;
import com.ticketintelligence.ticket_intelligence.repository.ProductRepository;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UserService userService;

    public ProductService(
            ProductRepository productRepository,
            UserService userService) {

        this.productRepository = productRepository;
        this.userService = userService;
    }

    // ============================================================
    // CREATE PRODUCT
    // ============================================================

    public Product createProduct(
            Product product,
            String username) {

        Seller seller = getSellerFromUsername(username);

        if (product.getName() == null ||
                product.getName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Product name is required."
            );
        }

        if (product.getSku() == null ||
                product.getSku().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "SKU is required."
            );
        }

        String sku = product.getSku()
                .trim()
                .toUpperCase();

        if (productRepository.existsBySkuAndSeller(
                sku,
                seller)) {

            throw new IllegalArgumentException(
                    "SKU already exists."
            );
        }

        if (product.getPrice() == null ||
                product.getPrice()
                        .compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    "Product price cannot be negative."
            );
        }

        if (product.getStockQuantity() < 0) {

            throw new IllegalArgumentException(
                    "Stock quantity cannot be negative."
            );
        }

        product.setProductId(generateProductId());

        product.setSku(sku);

        product.setName(
                product.getName().trim()
        );

        if (product.getStatus() == null ||
                product.getStatus().isBlank()) {

            product.setStatus("ACTIVE");

        } else {

            product.setStatus(
                    product.getStatus()
                            .trim()
                            .toUpperCase()
            );
        }

        product.setSeller(seller);

        return productRepository.save(product);
    }

    // ============================================================
    // GET ALL PRODUCTS
    // ============================================================

    public List<Product> getProducts(String username) {

        Seller seller = getSellerFromUsername(username);

        return productRepository.findBySeller(seller);
    }

    // ============================================================
    // GET SINGLE PRODUCT
    // ============================================================

    public Product getProduct(
            String productId,
            String username) {

        Seller seller = getSellerFromUsername(username);

        return productRepository
                .findByProductIdAndSeller(
                        productId,
                        seller
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found."
                        )
                );
    }

    // ============================================================
    // UPDATE PRODUCT
    // ============================================================

    public Product updateProduct(
            String productId,
            Product incoming,
            String username) {

        Product product = getProduct(
                productId,
                username
        );

        if (incoming.getName() != null &&
                !incoming.getName().isBlank()) {

            product.setName(
                    incoming.getName().trim()
            );
        }

        if (incoming.getCategory() != null) {

            product.setCategory(
                    incoming.getCategory().trim()
            );
        }

        if (incoming.getBrand() != null) {

            product.setBrand(
                    incoming.getBrand().trim()
            );
        }

        if (incoming.getDescription() != null) {

            product.setDescription(
                    incoming.getDescription().trim()
            );
        }

        if (incoming.getPrice() != null &&
                incoming.getPrice()
                        .compareTo(BigDecimal.ZERO) >= 0) {

            product.setPrice(
                    incoming.getPrice()
            );
        }

        if (incoming.getStockQuantity() >= 0) {

            product.setStockQuantity(
                    incoming.getStockQuantity()
            );
        }

        if (incoming.getWarrantyMonths() >= 0) {

            product.setWarrantyMonths(
                    incoming.getWarrantyMonths()
            );
        }

        if (incoming.getImageUrl() != null) {

            product.setImageUrl(
                    incoming.getImageUrl()
            );
        }

        if (incoming.getStatus() != null &&
                !incoming.getStatus().isBlank()) {

            product.setStatus(
                    incoming.getStatus()
                            .trim()
                            .toUpperCase()
            );
        }

        return productRepository.save(product);
    }

    // ============================================================
    // DELETE / DEACTIVATE PRODUCT
    // ============================================================

    public void deleteProduct(
            String productId,
            String username) {

        Product product = getProduct(
                productId,
                username
        );

        product.setStatus("INACTIVE");

        productRepository.save(product);
    }

    // ============================================================
    // RESOLVE SELLER FROM LOGGED-IN USER
    // ============================================================

    private Seller getSellerFromUsername(String username) {

        if (username == null ||
                username.isBlank()) {

            throw new IllegalArgumentException(
                    "Invalid authenticated user."
            );
        }

        // --------------------------------------------------------
        // CURRENT LOGIN SYSTEM
        //
        // authentication.getName()
        // returns something like:
        //
        // ADMIN-2958
        //
        // Find that User using customerId.
        // Then get the Seller linked to that User.
        // --------------------------------------------------------

        User admin =
                userService.findByCustomerId(
                        username.trim()
                );

        if (admin != null &&
                admin.getSeller() != null) {

            return admin.getSeller();
        }

        // --------------------------------------------------------
        // LEGACY LOGIN COMPATIBILITY
        //
        // Old format:
        //
        // SELLER-XXXX|phone
        // SELLER-XXXX|email
        // --------------------------------------------------------

        if (username.contains("|")) {

            String[] parts =
                    username.split("\\|", 2);

            if (parts.length == 2 &&
                    !parts[0].isBlank()) {

                return userService.findSeller(
                        parts[0].trim()
                );
            }
        }

        // --------------------------------------------------------
        // PHONE / EMAIL COMPATIBILITY
        // --------------------------------------------------------

        admin = userService.findByPhone(username);

        if (admin == null) {
            admin = userService.findByEmail(username);
        }

        if (admin != null &&
                admin.getSeller() != null) {

            return admin.getSeller();
        }

        throw new IllegalArgumentException(
                "Could not determine seller from logged-in administrator."
        );
    }

    // ============================================================
    // GENERATE UNIQUE PRODUCT ID
    // ============================================================

    private String generateProductId() {

        String id;

        do {

            id = "PROD-" +
                    UUID.randomUUID()
                            .toString()
                            .substring(0, 8)
                            .toUpperCase();

        } while (
                productRepository
                        .existsByProductId(id)
        );

        return id;
    }
}