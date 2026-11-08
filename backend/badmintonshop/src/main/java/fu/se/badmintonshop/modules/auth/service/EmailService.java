package fu.se.badmintonshop.modules.auth.service;

public interface EmailService {

    void sendPasswordResetEmail(String toEmail, String fullName, String resetUrl);
}

