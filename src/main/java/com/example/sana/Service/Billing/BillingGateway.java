package com.example.sana.Service.Billing;
import com.example.sana.Model.BillingOrder;
public interface BillingGateway {
    record Invoice(String id, String status, int amountHalalas, String currency, String checkoutUrl) { }
    Invoice createInvoice(BillingOrder order, String callbackUrl, String successUrl, String backUrl);
    Invoice fetchInvoice(String invoiceId);
}
