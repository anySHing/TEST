package com.example.membership;

import com.example.membership.ValidationGroups.MembershipAccumulateMarker;
import com.example.membership.ValidationGroups.MembershipAddMarker;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@Builder
@NoArgsConstructor(force = true)
public class MembershipRequest {
    @NotNull(groups = {MembershipAddMarker.class, MembershipAccumulateMarker.class})
    @Min(value = 0, groups = {MembershipAddMarker.class, MembershipAccumulateMarker.class})
    private final Integer point;

    @NotNull(groups = {MembershipAddMarker.class})
    private final MembershipType membershipType;
}
