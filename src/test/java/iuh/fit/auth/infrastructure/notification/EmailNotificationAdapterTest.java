package iuh.fit.auth.infrastructure.notification;

import iuh.fit.auth.application.port.notification.AsyncEmailPort;
import iuh.fit.auth.application.port.notification.EmailNotificationPort;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.IContext;
import org.thymeleaf.spring6.SpringTemplateEngine;

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
}
