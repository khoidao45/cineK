package com.codek.movieauthservice.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

  @Value("${app.sendgrid.api-key}")
  private String sendGridApiKey;

  @Value("${app.sendgrid.from-email}")
    private String fromEmail;

    @Value("${app.baseUrl}")
    private String baseUrl;

    /**
     * Sends an HTML verification email. Runs on a separate thread (@Async)
     * so it never blocks the register HTTP response.
     */
    @Async
    public void sendVerificationEmail(String toEmail, String username, String token) {
        String verifyLink = baseUrl + "/api/auth/verify-email?token=" + token;

        String html = """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                  <meta charset="UTF-8"/>
                  <style>
                    body { font-family: Arial, sans-serif; background:#f4f4f4; margin:0; padding:0; }
                    .container { max-width:560px; margin:40px auto; background:#fff;
                                 border-radius:8px; overflow:hidden;
                                 box-shadow:0 2px 8px rgba(0,0,0,.1); }
                    .header { background:#1a73e8; padding:32px; text-align:center; }
                    .header h1 { color:#fff; margin:0; font-size:24px; }
                    .body { padding:32px; color:#333; }
                    .body p { line-height:1.6; }
                    .btn { display:inline-block; margin:24px 0; padding:14px 32px;
                           background:#1a73e8; color:#fff; text-decoration:none;
                           border-radius:4px; font-size:16px; font-weight:bold; }
                    .footer { background:#f4f4f4; padding:16px; text-align:center;
                              font-size:12px; color:#999; }
                    .warning { font-size:13px; color:#888; margin-top:16px; }
                  </style>
                </head>
                <body>
                  <div class="container">
                    <div class="header"><h1>🎬 Movie App</h1></div>
                    <div class="body">
                      <p>Xin chào <strong>%s</strong>,</p>
                      <p>Cảm ơn bạn đã đăng ký tài khoản tại <strong>Movie App</strong>!<br/>
                         Vui lòng nhấn nút bên dưới để xác minh địa chỉ email của bạn:</p>
                      <div style="text-align:center;">
                        <a href="%s" class="btn">Xác Minh Email</a>
                      </div>
                      <p class="warning">
                        ⚠️ Link này sẽ chỉ sử dụng được <strong>một lần</strong>.<br/>
                        Nếu bạn không đăng ký tài khoản này, hãy bỏ qua email này.
                      </p>
                    </div>
                    <div class="footer">
                      © 2025 Movie App · Tất cả quyền được bảo lưu
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(username, verifyLink);

        try {
          Email from = new Email(fromEmail);
          Email to = new Email(toEmail);
          String subject = "[Movie App] Xác minh tài khoản của bạn";
          Content content = new Content("text/html", html);
          Mail mail = new Mail(from, subject, to, content);

          SendGrid sendGrid = new SendGrid(sendGridApiKey);
          Request request = new Request();
          request.setMethod(Method.POST);
          request.setEndpoint("mail/send");
          request.setBody(mail.build());

          Response response = sendGrid.api(request);
          if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            log.info("Verification email sent to: {}", toEmail);
          } else {
            log.error("Failed to send verification email to {}. status={}, body={}",
                toEmail, response.getStatusCode(), response.getBody());
          }
        } catch (IOException e) {
          log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
        }
    }
}
