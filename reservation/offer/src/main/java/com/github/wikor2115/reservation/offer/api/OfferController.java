package com.github.wikor2115.reservation.offer.api;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.github.wikor2115.reservation.offer.service.OfferService;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Offers", description = "Public offer browsing and admin offer management")
public class OfferController {
    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @GetMapping("/offers")
    @Operation(summary = "List active offers", description = "Returns offers visible to customers.")
    public List<OfferResponse> getActiveOffers(){
        return offerService.findActiveOffers().stream().map(this::toResponse).toList();
    }

    @GetMapping("/offers/{offerId}")
    @Operation(summary = "Get active offer", description = "Returns one active offer or 404 when missing or archived.")
    public OfferResponse getActiveOfferById(@PathVariable Long offerId) {
        return toResponse(offerService.findActiveOfferById(offerId));
    }

    @GetMapping("/admin/offers")
    @Operation(summary = "Admin list offers", description = "Returns active and archived offers for administration.")
    public List<OfferResponse> getAdminOffers() {
        return offerService.findAllOffers().stream().map(this::toResponse).toList();
    }

    @PostMapping("/admin/offers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Admin create offer", description = "Creates a new active offer.")
    public OfferResponse createOffer(@Valid @RequestBody CreateOfferRequest request){
        var offer = offerService.createOffer(request.name(), request.imageUrl(), request.description(), request.price());
        return toResponse(offer);
    }

    @PatchMapping("/admin/offers/{offerId}")
    @Operation(summary = "Admin update offer", description = "Partially updates an active offer.")
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
    @Operation(summary = "Admin archive offer", description = "Archives an offer so it disappears from public reads.")
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
