package tn.esprit.tests;

import tn.esprit.services.Payment;

public class PaymentTest {
    public static void main(String[] args) throws Exception {
        Payment.createPayment(
                15000,                              // amount
                "Order #123",                         // note
                "fathi",                               // firstName
                "mejri",                                // lastName
                "test@paymee.tn",                    // email
                "+21611222333",                       // phone
                "https://www.return_url.tn",         // returnUrl
                "https://www.cancel_url.tn",         // cancelUrl
                "https://www.webhook_url.tn",        // webhookUrl
                "244557"                              // orderId
        );
    }
}
