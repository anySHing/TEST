package com.example.membership;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor
public class MembershipDto {
    private final Long id;
    private final MembershipType membershipType;
}
