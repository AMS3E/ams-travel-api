package ams.asmtravel.keycloak.emailcode;

/**
 * The verification-code email: the AMS Travel card in HTML, with a plain-text part for mail clients that
 * want one. Built here so Keycloak needs no email template. The same mail serves a traveller registering and
 * a staff member signing in, so it says neither; the code and how long it lasts come from the step.
 */
final class EmailCodeMessage {

    private EmailCodeMessage() {
    }

    /** The subject carries the code, so it reads from the inbox list and the local checks can pick it up. */
    static String subject(String code) {
        return "Your AMS Travel verification code: " + code;
    }

    static String text(String code, int ttlMinutes) {
        return """
                Verify your email

                Use this verification code to continue with AMS Travel:

                %s

                This code expires in %d minutes.

                If you didn’t request this code, you can safely ignore this email.

                Explore Cambodia with confidence.
                © AMS Travel. All rights reserved.
                """.formatted(code, ttlMinutes);
    }

    static String html(String code, int ttlMinutes) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Verify your email</title></head>
                <body style="margin:0;padding:0;background:#f5f3ee;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f5f3ee;font-family:Arial,Helvetica,sans-serif;">
                  <tr><td align="center" style="padding:34px 14px 40px;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:560px;">
                      <tr><td align="center" style="padding:0 0 22px;font-size:15px;font-weight:bold;letter-spacing:4px;color:#6d28d9;">AMS&nbsp;TRAVEL</td></tr>
                      <tr><td style="background:#ffffff;border:1px solid #e4e1ee;border-radius:18px;padding:34px 7%%;">
                        <h1 style="margin:0 0 10px;font-size:22px;line-height:28px;color:#1b1830;">Verify your email</h1>
                        <p style="margin:0 0 26px;font-size:15px;line-height:24px;color:#4a4660;">Use the verification code below to continue with AMS Travel.</p>
                        <table role="presentation" align="center" cellpadding="0" cellspacing="0"><tr>
                          <td align="center" style="padding:18px 26px;background:#f5f0ff;border:1px solid #ddd0fe;border-radius:14px;font-size:34px;line-height:40px;font-weight:bold;letter-spacing:10px;color:#6d28d9;">%s</td>
                        </tr></table>
                        <p style="margin:18px 0 0;text-align:center;font-size:13px;color:#6d6983;">This code expires in %d minutes.</p>
                        <p style="margin:28px 0 0;padding:14px 16px;background:#f7f5fc;border:1px solid #e4e1ee;border-radius:12px;font-size:13px;line-height:20px;color:#4a4660;">If you didn’t request this code, you can safely ignore this email.</p>
                      </td></tr>
                      <tr><td align="center" style="padding:22px 12px 0;font-size:12px;line-height:19px;color:#6d6983;">Explore Cambodia with confidence.<br>© AMS Travel. All rights reserved.</td></tr>
                    </table>
                  </td></tr>
                </table>
                </body>
                </html>
                """.formatted(code, ttlMinutes);
    }
}
