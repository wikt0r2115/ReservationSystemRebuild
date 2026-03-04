package com.github.wikor2115.reservation.offer.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.github.wikor2115.reservation.offer.service.OfferService;
import java.util.List;

@RestController
@RequestMapping("/offers")
public class OfferController {
    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @GetMapping
    public List<OfferDTO> getActiveOffers(){
        return offerService.findActiveOffers().stream().map(offer -> new OfferDTO(
                offer.getId(),
                offer.getName(),
                offer.getImageUrl(),
                offer.getDescription(),
                offer.getPrice()
        )).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OfferDTO createOffer(@Valid @RequestBody CreateOfferRequest request){
        var offer = offerService.createOffer(request.name(), request.imageUrl(), request.description(), request.price());
        return new OfferDTO(
                offer.getId(),
                offer.getName(),
                offer.getImageUrl(),
                offer.getDescription(),
                offer.getPrice()
        );
    }

    @PutMapping("/{offerId}/archive")
    public OfferDTO archiveOffer(@PathVariable Long offerId) {
        var offer = offerService.archiveOffer(offerId);
        return new OfferDTO(
                offer.getId(),
                offer.getName(),
                offer.getImageUrl(),
                offer.getDescription(),
                offer.getPrice()
        );
    }
}
