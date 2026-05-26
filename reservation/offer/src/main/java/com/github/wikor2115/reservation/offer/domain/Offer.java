package com.github.wikor2115.reservation.offer.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import org.apache.commons.validator.routines.UrlValidator;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;

@Entity
public class Offer {
    private static final BigDecimal MAX_PRICE = new BigDecimal("99999.99");
    private static final UrlValidator URL_VALIDATOR = new UrlValidator(new String[] { "http", "https" });

    protected Offer() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String name;

    @NotBlank
    @Size(max = 2048)
    @URL
    @Column(nullable = false, length = 2048)
    private String imageUrl;

    @NotBlank
    @Size(max = 2048)
    @Column(nullable = false, length = 2048)
    private String description;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean archived = false;

    public static Offer create(String name, String imageUrl, String description, BigDecimal price) {
        Offer offer = new Offer();
        offer.validateName(name);
        offer.validateImageUrl(imageUrl);
        offer.validateDescription(description);
        offer.validatePrice(price);
        offer.name = name.trim();
        offer.imageUrl = imageUrl.trim();
        offer.description = description.trim();
        offer.price = price.setScale(2);
        return offer;
    }

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    public String getDescription() {
        return this.description;
    }

    public BigDecimal getPrice() {
        return this.price;
    }

    public boolean isArchived() {
        return this.archived;
    }

    public void archive() {
        if (this.archived)
            throw new IllegalStateException("Offer is archived");
        this.archived = true;
    }

    public void changePrice(BigDecimal newPrice) {
        this.validatePrice(newPrice);
        this.price = newPrice.setScale(2);
    }

    public void rename(String newName) {
        this.validateName(newName);
        newName = newName.trim();
        this.name = newName;
    }

    public void changeDescription(String newDescription) {
        this.validateDescription(newDescription);
        newDescription = newDescription.trim();
        this.description = newDescription;
    }

    public void changeImageUrl(String newImageUrl) {
        this.validateImageUrl(newImageUrl);
        newImageUrl = newImageUrl.trim();
        this.imageUrl = newImageUrl;
    }

    private void validateName(String name) {
        if (this.archived)
            throw new IllegalStateException("Offer is archived");
        if (name == null)
            throw new IllegalArgumentException("Name is null");
        name = name.trim();
        if (name.isEmpty())
            throw new IllegalArgumentException("Name is blank");
        if (name.length() > 255)
            throw new IllegalArgumentException("Name is too long");
        if (this.name == null)
            return;
    }

    private void validateImageUrl(String imageUrl) {
        if (this.archived)
            throw new IllegalStateException("Offer is archived");
        if (imageUrl == null)
            throw new IllegalArgumentException("Image URL is null");
        imageUrl = imageUrl.trim();
        if (imageUrl.isEmpty())
            throw new IllegalArgumentException("Image URL is blank");
        if (imageUrl.length() > 2048)
            throw new IllegalArgumentException("Image URL is too long");
        if (!isValidURL(imageUrl))
            throw new IllegalArgumentException("Image URL is not valid");
    }

    private void validateDescription(String description) {
        if (this.archived)
            throw new IllegalStateException("Offer is archived");
        if (description == null)
            throw new IllegalArgumentException("Description is null");
        description = description.trim();
        if (description.isEmpty())
            throw new IllegalArgumentException("Description is blank");
        if (description.length() > 2048)
            throw new IllegalArgumentException("Description is too long");
    }

    private void validatePrice(BigDecimal price) {
        if (this.archived)
            throw new IllegalStateException("Offer is archived");
        if (price == null)
            throw new IllegalArgumentException("Price is null");
        if (price.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Price must be greater than 0");
        if (price.compareTo(MAX_PRICE) > 0)
            throw new IllegalArgumentException("Price must not exceed 99999.99");
        if (price.scale() > 2)
            throw new IllegalArgumentException("Price must have at most 2 decimal places");
    }

    private static boolean isValidURL(String url) {
        return URL_VALIDATOR.isValid(url);
    }
}
