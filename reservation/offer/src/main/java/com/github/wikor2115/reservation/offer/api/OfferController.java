package com.github.wikor2115.reservation.offer.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.github.wikor2115.reservation.offer.service.OfferService;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class OfferController {
    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @GetMapping("/offers")
    public List<OfferResponse> getActiveOffers(){
        return offerService.findActiveOffers().stream().map(this::toResponse).toList();
    }

    @GetMapping("/offers/{offerId}")
    public OfferResponse getActiveOfferById(@PathVariable Long offerId) {
        return toResponse(offerService.findActiveOfferById(offerId));
    }

    @GetMapping("/admin/offers")
    public List<OfferResponse> getAdminOffers() {
        return offerService.findAllOffers().stream().map(this::toResponse).toList();
    }

    @PostMapping("/admin/offers")
    @ResponseStatus(HttpStatus.CREATED)
    public OfferResponse createOffer(@Valid @RequestBody CreateOfferRequest request){
        var offer = offerService.createOffer(request.name(), request.imageUrl(), request.description(), request.price());
        return toResponse(offer);
    }

    @PatchMapping("/admin/offers/{offerId}")
    public OfferResponse updateOffer(@PathVariable Long offerId, @Valid @RequestBody UpdateOfferRequest request) {
        return toResponse(offerService.updateOffer(
                offerId,
                request.name(),
                request.imageUrl(),
                request.description(),
                request.price()
        ));
    }

    @DeleteMapping("/admin/offers/{offerId}")
    public OfferResponse archiveOffer(@PathVariable Long offerId) {
        var offer = offerService.archiveOffer(offerId);
        return toResponse(offer);
    }

    private OfferResponse toResponse(com.github.wikor2115.reservation.offer.domain.Offer offer) {
        return new OfferResponse(
                offer.getId(),
                offer.getName(),
                offer.getImageUrl(),
                offer.getDescription(),
                offer.getPrice(),
                offer.isArchived()
        );
    }
}
