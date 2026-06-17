package iuh.fit.auth.service;

import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailNotificationServiceImpl implements EmailNotificationService {
    private final SpringTemplateEngine templateEngine;
    private final AsyncEmailService asyncEmailService;

    @Override
    public void sendVerificationEmail(String email, String verificationCode) {
        String subject = "Xác thực người dùng";
        Context context = new Context();
        context.setVariable("verificationCode", verificationCode);
        String content = this.templateEngine.process("verification", context);

        this.asyncEmailService.sendEmailSync(email, subject, content, false, true);
    }
}
