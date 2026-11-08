package fu.se.badmintonshop.modules.auth.service.impl;

import fu.se.badmintonshop.modules.auth.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Override
    public void sendPasswordResetEmail(String toEmail, String fullName, String resetUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "Badminton Shop");
            helper.setTo(toEmail);
            helper.setSubject("[Badminton Shop] Yêu cầu đặt lại mật khẩu của bạn");

            String displayName = (fullName != null && !fullName.isBlank()) ? fullName : "Quý khách";
            String htmlContent = buildResetPasswordHtml(displayName, resetUrl);

            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception ex) {
        }
    }

    private String buildResetPasswordHtml(String displayName, String resetUrl) {
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8">
              <style>
                body { margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f8fafc; color: #334155; }
                .container { max-width: 600px; margin: 30px auto; background-color: #ffffff; border-radius: 12px; border: 1px solid #e2e8f0; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.05); }
                .header { background-color: #173d5b; color: #ffffff; padding: 28px 32px; text-align: center; }
                .header h1 { margin: 0; font-size: 24px; font-weight: 700; letter-spacing: -0.5px; }
                .header p { margin: 6px 0 0; font-size: 13px; color: #93c5fd; }
                .content { padding: 36px 32px; }
                .greeting { font-size: 16px; font-weight: 600; color: #0f172a; margin-bottom: 16px; }
                .message { font-size: 14px; line-height: 1.7; color: #475569; margin-bottom: 28px; }
                .btn-container { text-align: center; margin: 32px 0; }
                .btn { display: inline-block; background-color: #0284c7; color: #ffffff !important; text-decoration: none; padding: 13px 30px; border-radius: 8px; font-size: 15px; font-weight: 600; box-shadow: 0 4px 10px rgba(2, 132, 199, 0.25); }
                .link-fallback { word-break: break-all; background-color: #f1f5f9; padding: 12px; border-radius: 6px; font-size: 12px; color: #64748b; margin-top: 20px; }
                .footer { background-color: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px 32px; text-align: center; font-size: 12px; color: #94a3b8; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>BADMINTON SHOP</h1>
                  <p>Đồng hành cùng đam mê cầu lông</p>
                </div>
                <div class="content">
                  <div class="greeting">Xin chào %s,</div>
                  <div class="message">
                    Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản Badminton Shop của bạn. Vui lòng bấm vào nút bên dưới để tiến hành thiết lập mật khẩu mới:
                  </div>
                  <div class="btn-container">
                    <a href="%s" class="btn" target="_blank">Đặt lại mật khẩu</a>
                  </div>
                  <div class="message" style="margin-bottom: 12px;">
                    <strong>Lưu ý:</strong> Liên kết này chỉ có hiệu lực trong vòng <strong>15 phút</strong>. Nếu bạn không gửi yêu cầu này, vui lòng bỏ qua email và mật khẩu của bạn vẫn được giữ nguyên an toàn.
                  </div>
                  <div class="link-fallback">
                    Nếu nút bấm trên không hoạt động, bạn có thể sao chép và dán trực tiếp liên kết sau vào trình duyệt:<br>
                    <a href="%s" style="color: #0284c7;">%s</a>
                  </div>
                </div>
                <div class="footer">
                  © 2026 Badminton Shop - SBA301 Management System. Mọi quyền được bảo lưu.
                </div>
              </div>
            </body>
            </html>
            """.formatted(displayName, resetUrl, resetUrl, resetUrl);
    }
}

