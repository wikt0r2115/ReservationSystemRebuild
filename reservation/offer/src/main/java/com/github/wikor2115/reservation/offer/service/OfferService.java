package com.github.wikor2115.reservation.offer.service;

import com.github.wikor2115.reservation.offer.domain.Offer;
import com.github.wikor2115.reservation.offer.repository.OfferRepository;


import java.util.List;
import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfferService {
    private final OfferRepository offerRepository;

    public OfferService(OfferRepository offerRepository) {
        this.offerRepository = offerRepository;
    }

    @Transactional
    public Offer createOffer(String name, String imageUrl, String description, BigDecimal price){
        Offer offer = Offer.create(name, imageUrl, description, price);
        return offerRepository.save(offer);
    }

    @Transactional(readOnly = true)
    public List<Offer> findActiveOffers(){
        return offerRepository.findByArchivedFalseOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Offer> findAllOffers(){
        return offerRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Offer findActiveOfferById(Long offerId) {
        return offerRepository.findByIdAndArchivedFalse(offerId)
                .orElseThrow(() -> new OfferNotFoundException(offerId));
    }

    @Transactional
    public Offer updateOffer(Long offerId, String name, String imageUrl, String description, BigDecimal price) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new OfferNotFoundException(offerId));

        if (name != null) {
            offer.rename(name);
        }
        if (imageUrl != null) {
            offer.changeImageUrl(imageUrl);
        }
        if (description != null) {
            offer.changeDescription(description);
        }
        if (price != null) {
            offer.changePrice(price);
        }

        return offerRepository.save(offer);
    }
    
    @Transactional
    public Offer archiveOffer(Long offerId){
        Offer offer = offerRepository.findById(offerId).orElseThrow(() -> new OfferNotFoundException(offerId));
        offer.archive();
        return offerRepository.save(offer);
    }

    @Transactional
    public Offer changeOfferPrice(Long offerId, BigDecimal newPrice){
        Offer offer = offerRepository.findById(offerId).orElseThrow(() -> new OfferNotFoundException(offerId));
        offer.changePrice(newPrice);
        return offerRepository.save(offer);
    }

    @Transactional
    public Offer renameOffer(Long offerId, String newName){
        Offer offer = offerRepository.findById(offerId).orElseThrow(() -> new OfferNotFoundException(offerId));
        offer.rename(newName);
        return offerRepository.save(offer);
    }
}   
