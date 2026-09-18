package com.ticketintelligence.ticket_intelligence.controller;

import com.ticketintelligence.ticket_intelligence.entity.Product;

import com.ticketintelligence.ticket_intelligence.service.ProductService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class ProductController {

    private final ProductService productService;

    public ProductController(
            ProductService productService) {

        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<?> createProduct(
            @RequestBody Product product,
            Authentication authentication) {

        try {

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(
                            productService.createProduct(
                                    product,
                                    authentication.getName()
                            )
                    );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getProducts(
            Authentication authentication) {

        return ResponseEntity.ok(
                productService.getProducts(
                        authentication.getName()
                )
        );
    }

    @GetMapping("/{productId}")
    public ResponseEntity<?> getProduct(
            @PathVariable String productId,
            Authentication authentication) {

        try {

            return ResponseEntity.ok(
                    productService.getProduct(
                            productId,
                            authentication.getName()
                    )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    @PutMapping("/{productId}")
    public ResponseEntity<?> updateProduct(
            @PathVariable String productId,
            @RequestBody Product product,
            Authentication authentication) {

        try {

            return ResponseEntity.ok(
                    productService.updateProduct(
                            productId,
                            product,
                            authentication.getName()
                    )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<?> deleteProduct(
            @PathVariable String productId,
            Authentication authentication) {

        try {

            productService.deleteProduct(
                    productId,
                    authentication.getName()
            );

            return ResponseEntity.ok(
                    "Product deactivated successfully."
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }
}
