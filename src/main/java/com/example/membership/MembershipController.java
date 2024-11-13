package com.example.membership;

import static com.example.membership.MembershipConstants.USER_ID_HEADER;

import java.util.List;

import com.example.membership.ValidationGroups.MembershipAccumulateMarker;
import com.example.membership.ValidationGroups.MembershipAddMarker;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MembershipController {

    private final MembershipService membershipService;

    @PostMapping("/v1/memberships")
    public ResponseEntity<MembershipDto> addMembership(
            @RequestHeader(USER_ID_HEADER) final String userId,
            @RequestBody @Validated(MembershipAddMarker.class) final MembershipRequest membershipRequest) {

        final MembershipDto membershipDto = membershipService.addMembership(
                userId, membershipRequest.getMembershipType(), membershipRequest.getPoint());

        return ResponseEntity.status(HttpStatus.CREATED).body(membershipDto);
    }

    @GetMapping("/v1/memberships")
    public ResponseEntity<List<MembershipDetailResponse>> getMembershipList(
            @RequestHeader(USER_ID_HEADER) final String userId) {
        return ResponseEntity.ok(membershipService.getMembershipList(userId));
    }

    @GetMapping("/v1/memberships/{id}")
    public ResponseEntity<MembershipDetailResponse> getMembership(
            @RequestHeader(USER_ID_HEADER) final String userId, @PathVariable("id") final Long membershipId) {
        return ResponseEntity.ok(membershipService.getMembership(membershipId, userId));
    }

    @DeleteMapping("/v1/memberships/{id}")
    public ResponseEntity<Void> removeMembership(
            @RequestHeader(USER_ID_HEADER) final String userId, @PathVariable("id") final Long membershipId) {
        membershipService.removeMembership(membershipId, userId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/v1/memberships/{id}/accumulate")
    public ResponseEntity<Void> accumulateMembershipPoint(
            @RequestHeader(USER_ID_HEADER) final String userId,
            @PathVariable("id") final Long id,
            @RequestBody @Validated(MembershipAccumulateMarker.class) final MembershipRequest membershipRequest) {
        membershipService.accumulateMembershipPoint(id, userId, membershipRequest.getPoint());
        return ResponseEntity.noContent().build();
    }
}
