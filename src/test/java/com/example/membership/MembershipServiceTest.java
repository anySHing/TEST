package com.example.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    private final String userId = "userId";
    private final MembershipType membershipType = MembershipType.NAVER;
    private final Integer point = 10000;
    private final Long membershipId = -1L;

    @InjectMocks // 테스트 대상(MembershipService를 테스트 함)
    private MembershipService target;

    @Mock
    private RatePointService ratePointService;

    @Mock
    private MembershipRepository membershipRepository;

    private Membership membership() {
        return Membership.builder()
                .id(1L)
                .userId(userId)
                .point(point)
                .membershipType(MembershipType.NAVER)
                .build();
    }

    @Nested
    @DisplayName("멤버십 등록 테스트")
    class MembershipRegistrationTest {

        @Test
        @DisplayName("이미 존재하는 멤버십 등록")
        void membershipAlreadyExists() {
            // given
            doReturn(Membership.builder().build())
                    .when(membershipRepository)
                    .findByUserIdAndMembershipType(userId, membershipType);

            // when
            final MembershipException result =
                    assertThrows(MembershipException.class, () -> target.addMembership(userId, membershipType, point));

            // then
            assertThat(result.getErrorResult()).isEqualTo(MembershipErrorResult.DUPLICATED_MEMBERSHIP_REGISTER);
        }

        @Test
        @DisplayName("멤버십 등록 성공")
        void saveMembership() {
            // given
            when(membershipRepository.findByUserIdAndMembershipType(userId, membershipType))
                    .thenReturn(null);
            when(membershipRepository.save(any(Membership.class))).thenReturn(membership());

            // when
            final MembershipDto result = target.addMembership(userId, membershipType, point);

            // then
            assertThat(result.getId()).isNotNull();
            assertThat(result.getMembershipType()).isEqualTo(MembershipType.NAVER);

            // verify
            verify(membershipRepository, times(1)).findByUserIdAndMembershipType(userId, membershipType);
            verify(membershipRepository, times(1)).save(any(Membership.class));
        }
    }

    @Nested
    @DisplayName("멤버십 목록 및 상세 조회 테스트")
    class MembershipListAndDetailTest {

        @Test
        @DisplayName("멤버십 목록 조회")
        void getMemberships() {
            // given
            doReturn(Arrays.asList(
                            membership().builder().build(),
                            membership().builder().build(),
                            membership().builder().build()))
                    .when(membershipRepository)
                    .findAllByUserId(userId);

            // when
            List<MembershipDetailResponse> result = target.getMembershipList(userId);

            // then
            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("상세조회 실패: 존재하지 않음")
        void getDetailNotFoundException() {
            // given
            doReturn(Optional.empty()).when(membershipRepository).findById(membershipId);

            // when
            final MembershipException result =
                    assertThrows(MembershipException.class, () -> target.getMembership(membershipId, userId));

            // then
            assertThat(result.getErrorResult()).isEqualTo(MembershipErrorResult.MEMBERSHIP_NOT_FOUND);
        }

        @Test
        @DisplayName("상세조회 실패: 본인이 아님")
        void getDetailNotAllowedException() {
            // given
            doReturn(Optional.empty()).when(membershipRepository).findById(membershipId);

            // when
            final MembershipException result =
                    assertThrows(MembershipException.class, () -> target.getMembership(membershipId, "notOwner"));

            // then
            assertThat(result.getErrorResult()).isEqualTo(MembershipErrorResult.MEMBERSHIP_NOT_FOUND);
        }

        @Test
        @DisplayName("상세조회 성공")
        void getDetailSuccessful() {
            // given
            doReturn(Optional.of(membership())).when(membershipRepository).findById(membershipId);

            // when
            final MembershipDetailResponse result = target.getMembership(membershipId, userId);

            // then
            assertThat(result.getMembershipType()).isEqualTo(MembershipType.NAVER);
            assertThat(result.getPoint()).isEqualTo(point);
        }
    }

    @Nested
    @DisplayName("멤버십 삭제 테스트")
    class MembershipDeletionTest {

        @Test
        @DisplayName("멤버십 삭제 실패: 존재하지 않음")
        void deleteNotFoundError() {
            // given
            when(membershipRepository.findById(membershipId)).thenReturn(Optional.empty());

            // when
            final MembershipException result =
                    assertThrows(MembershipException.class, () -> target.removeMembership(membershipId, userId));

            // then
            assertThat(result.getErrorResult()).isEqualTo(MembershipErrorResult.MEMBERSHIP_NOT_FOUND);
        }

        @Test
        @DisplayName("멤버십 삭제 실패: 본인이 아님")
        void deleteUnAuthorizedError() {
            // given
            final Membership membership = membership();
            when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));

            // when
            assertThatThrownBy(() -> target.removeMembership(membershipId, "notOwner"))
                    .isInstanceOf(MembershipException.class)
                    .hasFieldOrPropertyWithValue("errorResult", MembershipErrorResult.NOT_MEMBERSHIP_OWNER);

            // then
        }

        @Test
        @DisplayName("멤버십 삭제 성공")
        void deleteMembershipSuccessful() {
            // given
            final Membership membership = membership();
            when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));

            // when
            target.removeMembership(membershipId, userId);

            // then
        }
    }

    @Nested
    @DisplayName("멤버십 적립 테스트")
    class MembershipAccumulateTest {

        @Test
        @DisplayName("멤버십 적립 실패: 존재하지 않음")
        void accmulateNotFoundError() {
            // given
            when(membershipRepository.findById(membershipId)).thenReturn(Optional.empty());

            // when
            assertThatThrownBy(() -> target.accumulateMembershipPoint(membershipId, userId, 10000))
                    .isInstanceOf(MembershipException.class)
                    .hasFieldOrPropertyWithValue("errorResult", MembershipErrorResult.MEMBERSHIP_NOT_FOUND);
        }

        @Test
        @DisplayName("멤버십 적립 실패: 본인이 아님")
        void accmulateUnAuthorizedError() {
            // given
            final Membership membership = membership();
            when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));

            // when
            assertThatThrownBy(() -> target.accumulateMembershipPoint(membershipId, "notOwner", 10000))
                    .isInstanceOf(MembershipException.class)
                    .hasFieldOrPropertyWithValue("errorResult", MembershipErrorResult.NOT_MEMBERSHIP_OWNER);
        }

        @Test
        @DisplayName("멤버십 적립 성공")
        void accmulateSuccess() {
            // given
            final Membership membership = membership();
            when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));

            // when
            // then
            target.accumulateMembershipPoint(membershipId, userId, 10000);
        }
    }
}
