package com.groom.e_commerce.review.presentation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.e_commerce.review.application.service.ReviewService;
import com.groom.e_commerce.review.presentation.dto.request.CreateReviewRequest;
import com.groom.e_commerce.review.presentation.dto.request.UpdateReviewRequest;
import com.groom.e_commerce.review.presentation.dto.response.ReviewResponse;
import com.groom.e_commerce.user.domain.entity.user.UserRole;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

	private static final UUID USER_ID = UUID.randomUUID();
	private static final UUID REVIEW_ID = UUID.randomUUID();
	private static final UUID ORDER_ID = UUID.randomUUID();
	private static final UUID PRODUCT_ID = UUID.randomUUID();
	private static final UserRole USER_ROLE = UserRole.USER;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@Mock
	private ReviewService reviewService;

	@BeforeEach
	void setUp() {
		ReviewController controller =
			new ReviewController(reviewService) {
				@Override
				protected UUID getCurrentUserId() {
					return USER_ID;
				}

				@Override
				protected UserRole getCurrentUserRole() {
					return USER_ROLE;
				}
			};

		mockMvc = MockMvcBuilders
			.standaloneSetup(controller)
			.build();

		objectMapper = new ObjectMapper();
	}

	@Test
	void 리뷰_작성_성공() throws Exception {
		CreateReviewRequest request =
			new CreateReviewRequest(5, "색이 너무 이쁘다");

		when(reviewService.createReview(
			eq(ORDER_ID), eq(PRODUCT_ID), eq(USER_ID), any(CreateReviewRequest.class)
		)).thenReturn(mock(ReviewResponse.class));

		mockMvc.perform(post("/api/v1/reviews/{orderId}/items/{productId}", ORDER_ID, PRODUCT_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated());
	}

	@Test
	void 내_리뷰_조회_성공() throws Exception {
		when(reviewService.getReview(REVIEW_ID, USER_ID))
			.thenReturn(mock(ReviewResponse.class));

		mockMvc.perform(get("/api/v1/reviews/me/{reviewId}", REVIEW_ID))
			.andExpect(status().isOk());
	}

	@Test
	void 리뷰_수정_성공() throws Exception {
		UpdateReviewRequest request =
			new UpdateReviewRequest("수정된 리뷰", 4);

		when(reviewService.updateReview(
			eq(REVIEW_ID), eq(USER_ID), any(UpdateReviewRequest.class)
		)).thenReturn(mock(ReviewResponse.class));

		mockMvc.perform(put("/api/v1/reviews/{reviewId}", REVIEW_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk());
	}

	@Test
	void 리뷰_삭제_성공() throws Exception {
		mockMvc.perform(delete("/api/v1/reviews/{reviewId}", REVIEW_ID))
			.andExpect(status().isNoContent());

		verify(reviewService).deleteReview(REVIEW_ID, USER_ID, USER_ROLE);
	}

	@Test
	void 리뷰_좋아요_성공() throws Exception {
		when(reviewService.likeReview(REVIEW_ID, USER_ID))
			.thenReturn(10);

		mockMvc.perform(post("/api/v1/reviews/{reviewId}/like", REVIEW_ID))
			.andExpect(status().isOk())
			.andExpect(content().string("10"));
	}

	@Test
	void 리뷰_좋아요_취소_성공() throws Exception {
		when(reviewService.unlikeReview(REVIEW_ID, USER_ID))
			.thenReturn(9);

		mockMvc.perform(delete("/api/v1/reviews/{reviewId}/like", REVIEW_ID))
			.andExpect(status().isOk())
			.andExpect(content().string("9"));
	}
}
