package com.example.membership;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Builder
public class MembershipCreateDto {
    private final Long id;
    private final MembershipType membershipType;
}
