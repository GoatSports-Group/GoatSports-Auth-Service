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
    void bookingTicketEmbedsTheQrAsAPngAndLinksToTheBooking() {
        SpringTemplateEngine templates = mock(SpringTemplateEngine.class);
        AsyncEmailPort mail = mock(AsyncEmailPort.class);
        when(templates.process(eq("booking-ticket"), any(IContext.class))).thenReturn("<html/>");
        EmailNotificationAdapter adapter = new EmailNotificationAdapter(templates, mail);
        ReflectionTestUtils.setField(adapter, "clientUrl", "https://goatsports.vn");
        UUID bookingId = UUID.randomUUID();

        adapter.sendBookingTicket("player@goatsports.vn", "Minh Đạt", new EmailNotificationPort.BookingTicketMail(
                bookingId, "GS7K2M9Q", "GOAT", "Bóng đá", null, LocalDate.of(2026, 10, 17),
                LocalTime.of(14, 0), LocalTime.of(15, 0), 60_000.0, 140_000.0, "GSQR.v1.signed"));

        ArgumentCaptor<IContext> context = ArgumentCaptor.forClass(IContext.class);
        verify(templates).process(eq("booking-ticket"), context.capture());
        assertThat(context.getValue().getVariable("date")).isEqualTo("Thứ Bảy, 17/10/2026");
        assertThat(context.getValue().getVariable("detailUrl"))
                .isEqualTo("https://goatsports.vn/booking/detail/" + bookingId);
        ArgumentCaptor<byte[]> png = ArgumentCaptor.forClass(byte[].class);
        verify(mail).sendHtmlWithInlineImage(eq("player@goatsports.vn"),
                eq("Vé đặt sân GS7K2M9Q · 14:00 – 15:00 17/10/2026"), eq("<html/>"),
                eq(EmailNotificationAdapter.TICKET_QR_ID), png.capture());
        // Chữ ký file PNG: \x89 P N G.
        assertThat(png.getValue()).startsWith((byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G');
    }
}
