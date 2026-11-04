package iuh.fit.auth.infrastructure.notification;

import iuh.fit.auth.application.port.notification.AsyncEmailPort;
import iuh.fit.auth.application.port.notification.EmailNotificationPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.IContext;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailNotificationAdapterTest {
    @Test
    void supportRequestGoesToTheSupportInboxAndRepliesGoToThePlayer() {
        SpringTemplateEngine templates = mock(SpringTemplateEngine.class);
        AsyncEmailPort mail = mock(AsyncEmailPort.class);
        when(templates.process(eq("support-request"), any(IContext.class))).thenReturn("<html/>");
        EmailNotificationAdapter adapter = new EmailNotificationAdapter(templates, mail);
        ReflectionTestUtils.setField(adapter, "supportInbox", "goatsports1312@gmail.com");

        adapter.sendSupportRequest(new EmailNotificationPort.SupportRequestMail(
                "Minh Đạt", "player@goatsports.vn", "Hủy sân & hoàn tiền", "Chưa nhận tiền hoàn", "Nội dung"));

        verify(mail).sendHtmlWithReplyTo("goatsports1312@gmail.com", "player@goatsports.vn",
                "[Hỗ trợ GOAT Sports] Hủy sân & hoàn tiền · Chưa nhận tiền hoàn", "<html/>");
    }

    @Test
    void bookingTicketLinksTheQrImageWithoutAttachmentsAndLinksToTheBooking() {
        SpringTemplateEngine templates = mock(SpringTemplateEngine.class);
        AsyncEmailPort mail = mock(AsyncEmailPort.class);
        when(templates.process(eq("booking-ticket"), any(IContext.class))).thenReturn("<html/>");
        EmailNotificationAdapter adapter = new EmailNotificationAdapter(templates, mail);
        ReflectionTestUtils.setField(adapter, "clientUrl", "https://goatsports.vn");
        ReflectionTestUtils.setField(adapter, "apiPublicUrl", "https://api.goatsports.vn");
        UUID bookingId = UUID.randomUUID();

        adapter.sendBookingTicket("player@goatsports.vn", "Minh Đạt", new EmailNotificationPort.BookingTicketMail(
                bookingId, "GS7K2M9Q", "GOAT", "Bóng đá", null, LocalDate.of(2026, 10, 17),
                LocalTime.of(14, 0), LocalTime.of(15, 0), 60_000.0, 140_000.0, "GSQR.v1.id.1.sig+/="));

        ArgumentCaptor<IContext> context = ArgumentCaptor.forClass(IContext.class);
        verify(templates).process(eq("booking-ticket"), context.capture());
        assertThat(context.getValue().getVariable("date")).isEqualTo("Thứ Bảy, 17/10/2026");
        assertThat(context.getValue().getVariable("detailUrl"))
                .isEqualTo("https://goatsports.vn/booking/detail/" + bookingId);
        assertThat(context.getValue().getVariable("qrImageUrl")).isEqualTo(
                "https://api.goatsports.vn/venue-service/api/v1/venues/ticket-qr?code=GSQR.v1.id.1.sig%2B%2F%3D");
        verify(mail).sendEmailSync("player@goatsports.vn",
                "Vé đặt sân GS7K2M9Q · 14:00 – 15:00 17/10/2026", "<html/>", false, true);
    }
}
