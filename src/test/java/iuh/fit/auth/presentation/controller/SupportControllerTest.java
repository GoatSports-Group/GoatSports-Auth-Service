package iuh.fit.auth.presentation.controller;

import iuh.fit.auth.application.port.notification.EmailNotificationPort;
import iuh.fit.auth.application.service.AuthRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SupportControllerTest {
    private final EmailNotificationPort mail = mock(EmailNotificationPort.class);
    private final AuthRateLimiter limiter = mock(AuthRateLimiter.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new SupportController(mail, limiter)).build();
    }

    @Test
    void validRequestIsForwardedToTheSupportInboxWithTheTopicLabel() throws Exception {
        mvc.perform(post("/api/v1/support/requests").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fullName":"  Minh Đạt ","email":"player@goatsports.vn","topic":"REFUND",
                         "subject":"Chưa nhận tiền hoàn","message":"Mình hủy sân hôm qua mà chưa thấy tiền về."}
                        """))
                .andExpect(status().isAccepted());

        verify(limiter).checkSupportRequest("player@goatsports.vn");
        ArgumentCaptor<EmailNotificationPort.SupportRequestMail> sent =
                ArgumentCaptor.forClass(EmailNotificationPort.SupportRequestMail.class);
        verify(mail).sendSupportRequest(sent.capture());
        assertThat(sent.getValue().fullName()).isEqualTo("Minh Đạt");
        assertThat(sent.getValue().topic()).isEqualTo("Hủy sân & hoàn tiền");
    }

    @Test
    void unknownTopicOrShortMessageIsRejectedWithoutSendingMail() throws Exception {
        mvc.perform(post("/api/v1/support/requests").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fullName":"Minh Đạt","email":"player@goatsports.vn","topic":"SPAM",
                         "subject":"Xin chào","message":"ngắn"}
                        """))
                .andExpect(status().isBadRequest());

        verify(mail, never()).sendSupportRequest(any());
    }
}
