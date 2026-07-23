package backend.agerdon.domain.member.controller;

import backend.agerdon.domain.member.dto.request.FavoritePlaceRequest;
import backend.agerdon.domain.member.dto.request.FavoritePlaceUpdateRequest;
import backend.agerdon.domain.member.dto.response.FavoritePlaceResponse;
import backend.agerdon.domain.member.entity.FavoriteType;
import backend.agerdon.domain.member.service.MemberFavoritePlaceService;
import backend.agerdon.global.response.ApiResponse;
import backend.agerdon.global.security.principal.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Member - Favorite Place", description = "마이페이지 내 주소 및 즐겨찾는 장소 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members/me")
public class MemberFavoritePlaceController {

    private final MemberFavoritePlaceService memberFavoritePlaceService;

    @Operation(
            summary = "즐겨찾기 장소 추가",
            description = "장소를 즐겨찾기로 등록합니다. favoriteType을 HOME으로 등록하면 '내 주소'가 되며, 기존 HOME은 GENERAL로 자동 변경됩니다. "
                    + "이미 즐겨찾기된 장소를 다시 등록하면 타입/별칭이 갱신됩니다."
    )
    @PostMapping("/favorite-places")
    public ResponseEntity<ApiResponse<FavoritePlaceResponse>> addFavorite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FavoritePlaceRequest request
    ) {
        FavoritePlaceResponse response = memberFavoritePlaceService.addFavorite(currentMemberId(userDetails), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("즐겨찾기 장소가 등록되었습니다.", response));
    }

    @Operation(summary = "즐겨찾기 장소 목록 조회", description = "type을 지정하면 HOME 또는 GENERAL만 필터링해서 조회합니다.")
    @GetMapping("/favorite-places")
    public ResponseEntity<ApiResponse<List<FavoritePlaceResponse>>> listFavorites(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "즐겨찾기 타입 필터 (HOME | GENERAL)")
            @RequestParam(required = false) FavoriteType type
    ) {
        List<FavoritePlaceResponse> response = memberFavoritePlaceService.listFavorites(currentMemberId(userDetails), type);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "즐겨찾기 장소 단건 조회")
    @GetMapping("/favorite-places/{favoriteId}")
    public ResponseEntity<ApiResponse<FavoritePlaceResponse>> getFavorite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long favoriteId
    ) {
        FavoritePlaceResponse response = memberFavoritePlaceService.getFavorite(currentMemberId(userDetails), favoriteId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "내 주소 조회", description = "HOME으로 태그된 즐겨찾기 장소를 조회합니다. 등록된 주소가 없으면 data는 null입니다.")
    @GetMapping("/address")
    public ResponseEntity<ApiResponse<FavoritePlaceResponse>> getAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        FavoritePlaceResponse response = memberFavoritePlaceService.getHomeAddress(currentMemberId(userDetails));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "즐겨찾기 장소 수정",
            description = "즐겨찾기 별칭 또는 타입을 수정합니다. favoriteType을 HOME으로 변경하면 기존 HOME은 GENERAL로 자동 변경됩니다."
    )
    @PatchMapping("/favorite-places/{favoriteId}")
    public ResponseEntity<ApiResponse<FavoritePlaceResponse>> updateFavorite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long favoriteId,
            @RequestBody FavoritePlaceUpdateRequest request
    ) {
        FavoritePlaceResponse response = memberFavoritePlaceService.updateFavorite(
                currentMemberId(userDetails), favoriteId, request
        );
        return ResponseEntity.ok(ApiResponse.success("즐겨찾기 장소가 수정되었습니다.", response));
    }

    @Operation(summary = "즐겨찾기 장소 삭제")
    @DeleteMapping("/favorite-places/{favoriteId}")
    public ResponseEntity<ApiResponse<Void>> deleteFavorite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long favoriteId
    ) {
        memberFavoritePlaceService.deleteFavorite(currentMemberId(userDetails), favoriteId);
        return ResponseEntity.ok(ApiResponse.success("즐겨찾기 장소가 삭제되었습니다.", null));
    }

    private Long currentMemberId(CustomUserDetails userDetails) {
        return userDetails.getMember().getId();
    }
}
