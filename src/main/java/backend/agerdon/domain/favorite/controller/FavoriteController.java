package backend.agerdon.domain.favorite.controller;

import backend.agerdon.domain.favorite.dto.FavoriteRequest;
import backend.agerdon.domain.favorite.dto.FavoriteResponse;
import backend.agerdon.domain.favorite.service.FavoriteService;
import backend.agerdon.global.security.principal.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    // 1. 즐겨찾기 추가
    @PostMapping
    public FavoriteResponse addFavorite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FavoriteRequest request
    ) {
        return favoriteService.addFavorite(userDetails.getMember().getId(), request.getPlaceId());
    }

    // 2. 즐겨찾기 삭제
    @DeleteMapping("/{favoriteId}")
    public void removeFavorite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long favoriteId
    ) {
        favoriteService.removeFavorite(userDetails.getMember().getId(), favoriteId);
    }
}
