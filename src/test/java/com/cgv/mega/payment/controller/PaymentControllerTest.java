package com.cgv.mega.payment.controller;

import com.cgv.mega.common.enums.Role;
import com.cgv.mega.payment.dto.PaymentCompleteRequest;
import com.cgv.mega.payment.service.PaymentService;
import com.cgv.mega.util.CustomMockUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @Nested
    class 결제_검증 {

        @Test
        @CustomMockUser(id = 1L, name = "user", email = "a@b.com", role = Role.USER)
        void 검증_성공() throws Exception {
            PaymentCompleteRequest request = new PaymentCompleteRequest("payment-id",
                    BigDecimal.valueOf(20000), 5L
            );

            mockMvc.perform(MockMvcRequestBuilders.post("/api/payments/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andDo(print());
        }

        @Test
        void 필수값_누락_400반환() throws Exception {
            PaymentCompleteRequest request = new PaymentCompleteRequest(
                    "payment-id",
                    null, 5L
            );

            mockMvc.perform(MockMvcRequestBuilders.post("/api/payments/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }
}