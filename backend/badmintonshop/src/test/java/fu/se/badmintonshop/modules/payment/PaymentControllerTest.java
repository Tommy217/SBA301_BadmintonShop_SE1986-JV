package fu.se.badmintonshop.modules.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import fu.se.badmintonshop.config.SecurityConfig;
import fu.se.badmintonshop.modules.payment.controller.PaymentController;
import fu.se.badmintonshop.modules.payment.dto.request.ConfirmWebhookRequest;
import fu.se.badmintonshop.modules.payment.dto.request.CreatePaymentRequest;
import fu.se.badmintonshop.modules.payment.dto.request.PayOSWebhookData;
import fu.se.badmintonshop.modules.payment.dto.request.PayOSWebhookRequest;
import fu.se.badmintonshop.modules.payment.dto.response.CreatePaymentResponse;
import fu.se.badmintonshop.modules.payment.dto.response.PaymentDetailResponse;
import fu.se.badmintonshop.modules.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import({
    SecurityConfig.class,
    fu.se.badmintonshop.modules.auth.security.JwtProperties.class,
    fu.se.badmintonshop.modules.auth.security.JwtService.class,
    fu.se.badmintonshop.modules.auth.security.JwtAuthenticationFilter.class
})
@org.springframework.security.test.context.support.WithMockUser(roles = "ADMIN")
class PaymentControllerTest {
    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testCreatePayment_Endpoint() throws Exception {
        CreatePaymentResponse mockResponse = CreatePaymentResponse.builder()
                .paymentId(1L)
                .paymentCode("PAY-20261105-00001")
                .payosOrderCode(100001L)
                .checkoutUrl("https://pay.payos.vn/web/test")
                .qrCodeUrl("https://img.vietqr.io/image/test.png")
                .amount(new BigDecimal("1000000"))
                .status("pending")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        when(paymentService.createPayment(any(CreatePaymentRequest.class))).thenReturn(mockResponse);

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .orderId(10L)
                .amount(new BigDecimal("1000000"))
                .description("Don 10")
                .build();

        mockMvc.perform(post("/api/payments/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.paymentId").value(1))
                .andExpect(jsonPath("$.data.paymentCode").value("PAY-20261105-00001"))
                .andExpect(jsonPath("$.data.checkoutUrl").value("https://pay.payos.vn/web/test"));
    }

    @Test
    void testGetPaymentById_Endpoint() throws Exception {
        PaymentDetailResponse mockDetail = PaymentDetailResponse.builder()
                .id(1L)
                .orderId(10L)
                .status("pending")
                .amount(new BigDecimal("1000000"))
                .gatewayTxnRef("100001")
                .build();

        when(paymentService.getPaymentById(1L)).thenReturn(mockDetail);

        mockMvc.perform(get("/api/payments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("pending"));
    }

    @Test
    void testPayosWebhook_Endpoint() throws Exception {
        doNothing().when(paymentService).handlePayOSWebhook(any(PayOSWebhookRequest.class));

        PayOSWebhookData data = PayOSWebhookData.builder()
                .orderCode(100001L)
                .amount(new BigDecimal("1000000"))
                .reference("FT2611019999")
                .build();

        PayOSWebhookRequest request = PayOSWebhookRequest.builder()
                .code("00")
                .desc("success")
                .data(data)
                .signature("sample_signature")
                .build();

        mockMvc.perform(post("/api/payments/payos/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("00"));
    }

    @Test
    void testConfirmWebhook_Endpoint() throws Exception {
        doNothing().when(paymentService).confirmWebhook(any());

        ConfirmWebhookRequest request = ConfirmWebhookRequest.builder()
                .webhookUrl("https://example.com/api/payments/payos/webhook")
                .build();

        mockMvc.perform(post("/api/payments/payos/confirm-webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}

