package com.example.membership;

import static com.example.membership.MembershipConstants.USER_ID_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.google.gson.Gson;

@ExtendWith(MockitoExtension.class)
class MembershipControllerTest {
    @InjectMocks
    private MembershipController target;

    @Mock
    private MembershipService membershipService;

    private MockMvc mockMvc;
    private Gson gson;

    @BeforeEach
    void initialize() {
        gson = new Gson();
        mockMvc = MockMvcBuilders.standaloneSetup(target)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MembershipRequest membershipRequest(final Integer point) {
        return MembershipRequest.builder().point(point).build();
    }

    @Test
    @DisplayName("mockMvc가 널이 아님")
    void mock_is_not_null() throws Exception {
        assertThat(target).isNotNull();
        assertThat(mockMvc).isNotNull();
    }

    @Test
    @DisplayName("멤버십 등록 실패 / 사용자 식별 값이 헤더에 없음")
    void idNotFoundException() throws Exception {
        // given
        final String url = "/api/v1/memberships";

        // when
        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(url)
                .content(gson.toJson(MembershipRequest.builder()
                        .point(10000)
                        .membershipType(MembershipType.NAVER)
                        .build()))
                .contentType(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("멤버십 등록 실패 / 포인트가 NULL")
    void pointNullPointerException() throws Exception {
        // given
        final String url = "/api/v1/memberships";

        // when
        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(url)
                .header(USER_ID_HEADER, "123123123")
                .content(gson.toJson(MembershipRequest.builder()
                        .point(null)
                        .membershipType(MembershipType.NAVER)
                        .build()))
                .contentType(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("멤버십 등록 실패 / 포인트가 음수")
    void negativePointException() throws Exception {
        // given
        final String url = "/api/v1/memberships";

        // when
        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(url)
                .header(USER_ID_HEADER, "123")
                .content(gson.toJson(MembershipRequest.builder()
                        .point(-1)
                        .membershipType(MembershipType.NAVER)
                        .build()))
                .contentType(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("멤버십 등록 실패 / 멤버십 종류가 NULL")
    void typeNullPointerException() throws Exception {
        // given
        final String url = "/api/v1/memberships";

        // when
        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(url)
                .header(USER_ID_HEADER, "12345")
                .content(gson.toJson(MembershipRequest.builder()
                        .point(10000)
                        .membershipType(null)
                        .build()))
                .contentType(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("멤버십 등록 실패 / MemberService에서 에러 Throw")
    void ErrorThrownFromService() throws Exception {
        // given
        final String url = "/api/v1/memberships";
        doThrow(new MembershipException(MembershipErrorResult.DUPLICATED_MEMBERSHIP_REGISTER))
                .when(membershipService)
                .addMembership("12345", MembershipType.NAVER, 10000);

        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(url)
                .header(USER_ID_HEADER, "12345")
                .content(gson.toJson(MembershipRequest.builder()
                        .point(10000)
                        .membershipType(MembershipType.NAVER)
                        .build()))
                .contentType(MediaType.APPLICATION_JSON));

        // then

        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("멤버십 등록 성공")
    void saveSuccess() throws Exception {
        // given
        final String url = "/api/v1/memberships";
        final MembershipDto membershipDto = MembershipDto.builder()
                .id(-1L)
                .membershipType(MembershipType.NAVER)
                .build();

        doReturn(membershipDto).when(membershipService).addMembership("12345", MembershipType.NAVER, 10000);

        // when
        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(url)
                .header(USER_ID_HEADER, "12345")
                .content(gson.toJson(MembershipRequest.builder()
                        .point(10000)
                        .membershipType(MembershipType.NAVER)
                        .build()))
                .contentType(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isCreated());

        final MembershipDto response = gson.fromJson(
                resultActions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
                MembershipDto.class);

        assertThat(response.getMembershipType()).isEqualTo(MembershipType.NAVER);
        assertThat(response.getId()).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("invalidMembershipAddParameter")
    @DisplayName("멤버십 등록 실패 / 잘못된 파라미터")
    void invalidParams(final Integer point, final MembershipType membershipType) throws Exception {
        // given
        final String url = "/api/v1/memberships";

        // when
        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(url)
                .header(USER_ID_HEADER, "12345")
                .content(gson.toJson(MembershipRequest.builder()
                        .point(point)
                        .membershipType(membershipType)
                        .build()))
                .contentType(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isBadRequest());
    }

    public static Stream<Arguments> invalidMembershipAddParameter() {
        return Stream.of(
                Arguments.of(null, MembershipType.NAVER),
                Arguments.of(-1, MembershipType.NAVER),
                Arguments.of(10000, null));
    }

    @Test
    @DisplayName("멤버십 목록 조회 실패 / id가 헤더가 없음")
    void unauthorizedException() throws Exception {
        // given
        final String url = "/api/v1/memberships";

        // when
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(url));

        // then
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("멤버십 목록 조회 성공")
    void getSuccessful() throws Exception {
        // given
        final String url = "/api/v1/memberships";
        doReturn(Arrays.asList(
                MembershipDetailResponse.builder().build(),
                MembershipDetailResponse.builder().build(),
                MembershipDetailResponse.builder().build()))
                .when(membershipService)
                .getMembershipList("12345");

        // when
        ResultActions resultActions =
                mockMvc.perform(MockMvcRequestBuilders.get(url).header(USER_ID_HEADER, "12345"));

        // then
        resultActions.andExpect(status().isOk());
    }

    @Test
    @DisplayName("멤버십 상세 조회 실패 / id 없음")
    void getDetailIdNotFoundException() throws Exception {
        // given
        final String url = "/api/v1/memberships";

        // when
        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get(url));

        // then
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("멤버십 상세 조회 실패 / 멤버십이 존재하지 않음")
    void getDetailUnAuthorizedException() throws Exception {
        // given
        final String url = "/api/v1/memberships/-1";
        doThrow(new MembershipException(MembershipErrorResult.MEMBERSHIP_NOT_FOUND))
                .when(membershipService)
                .getMembership(-1L, "12345");

        // when
        final ResultActions resultActions =
                mockMvc.perform(MockMvcRequestBuilders.get(url).header(USER_ID_HEADER, "12345"));

        // then
        resultActions.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("멤버십 상세 조회 성공")
    void getDetailSuccessful() throws Exception {
        // given
        final String url = "/api/v1/memberships/-1";
        doReturn(MembershipDetailResponse.builder().build())
                .when(membershipService)
                .getMembership(-1L, "12345");
        // when
        final ResultActions resultActions =
                mockMvc.perform(MockMvcRequestBuilders.get(url).header(USER_ID_HEADER, "12345"));

        // then
        resultActions.andExpect(status().isOk());
    }

    @Test
    @DisplayName("멤버십 삭제 실패: 헤더에 id 없음")
    void deleteUnAuthorizedException() throws Exception {
        // given
        final String url = "/api/v1/memberships/-1";

        // when
        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.delete(url));

        // then
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("멤버십 삭제 성공")
    void deleteSuccessful() throws Exception {
        // given
        final String url = "/api/v1/memberships/-1";

        // when
        final ResultActions resultActions =
                mockMvc.perform(MockMvcRequestBuilders.delete(url).header(USER_ID_HEADER, "12345"));

        // then
        resultActions.andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("멤버십 적립 실패: 사용자 식별 값이 헤더에 없음")
    void accumulateUnAuthorizedException() throws Exception {
        // given
        final String url = "/api/v1/memberships/-1/accumulate";

        // when
        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(url)
                .content(gson.toJson(membershipRequest(10000)))
                .contentType(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("멤버십 적립 실패: 포인트가 음수")
    void accumulateNegativePointException() throws Exception {
        // given
        final String url = "/api/v1/memberships/-1/accumulate";

        // when
        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(url)
                .header(USER_ID_HEADER, "12345")
                .content(gson.toJson(membershipRequest(-1)))
                .contentType(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("멤버십 적립 성공")
    void accumulateSuccessful() throws Exception {
        // given
        final String url = "/api/v1/memberships/-1/accumulate";

        // when
        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post(url)
                .header(USER_ID_HEADER, "12345")
                .content(gson.toJson(membershipRequest(10000)))
                .contentType(MediaType.APPLICATION_JSON));

        // then
        resultActions.andExpect(status().isNoContent());
    }
}
