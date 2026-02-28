package com.github.wikor2115.reservation.offer.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;


import java.math.BigDecimal;

@Entity
public class Offer {
    protected Offer(){}
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
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

    public Long getId(){return this.id;}
    public String getName(){return this.name;}
    public String getImageUrl(){return this.imageUrl;}
    public String getDescription(){return this.description;}
    public BigDecimal getPrice(){return this.price;}
    public boolean getArchived(){return this.archived;}

    static private final BigDecimal MAX_PRICE = new BigDecimal("99999.99");

    public void archive(){
        if(this.archived)
            throw new IllegalStateException("Offer is archived");
        this.archived = true;
    }

    public void changePrice(BigDecimal newPrice){

        if(this.archived)
            throw new IllegalStateException("Offer is archived");
        if(newPrice == null)
            throw new IllegalArgumentException("Price is null");
        if(newPrice.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Price must be greater than 0");
        if(newPrice.compareTo(MAX_PRICE) > 0)
            throw new IllegalArgumentException("Price must not exceed 99999.99");
        this.price = newPrice;
    }
    public void rename(String newName){
        if(this.archived)
            throw new IllegalStateException("Offer is archived");
        if(newName == null)
            throw new IllegalArgumentException("Name is null");
        newName = newName.trim();
        if(newName.isEmpty())
            throw new IllegalArgumentException("Name is blank");
        if(newName.length() > 255)
            throw new IllegalArgumentException("Name is too long");
        if(this.name.equals(newName))
            throw new IllegalArgumentException("Name is the same");
        this.name = newName;
    }
    public void changeDescription(String newDescription){
        if(this.archived)
            throw new IllegalStateException("Offer is archived");
        if(newDescription == null)
            throw new IllegalArgumentException("Description is null");
        newDescription = newDescription.trim();
        if(newDescription.isEmpty())
            throw new IllegalArgumentException("Description is blank");
        if(newDescription.length() > 2048)
            throw new IllegalArgumentException("Description is too long");
        if(this.description.equals(newDescription))
            throw new IllegalArgumentException("Description is the same");
        this.description = newDescription;
    }
}
