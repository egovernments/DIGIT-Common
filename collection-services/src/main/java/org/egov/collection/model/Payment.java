package org.egov.collection.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import org.egov.collection.model.enums.InstrumentStatusEnum;
import org.egov.collection.model.enums.PaymentModeEnum;
import org.egov.collection.model.enums.PaymentStatusEnum;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class Payment {

    @Size(max=64)
    @JsonProperty("id")
    private String id;

    @NotNull
    @Size(max=64)
    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("totalDue")
    private BigDecimal totalDue;

    @NotNull
    @JsonProperty("totalAmountPaid")
    private BigDecimal totalAmountPaid;

    
    @Size(max=128)
    @JsonProperty("transactionNumber")
    private String transactionNumber;

    @JsonProperty("transactionDate")
    private Long transactionDate;

    @NotNull
    @JsonProperty("paymentMode")
    private PaymentModeEnum paymentMode;

    
    @JsonProperty("instrumentDate")
    private Long instrumentDate;

    
    @Size(max=128)
    @JsonProperty("instrumentNumber")
    private String instrumentNumber;

    @JsonProperty("instrumentStatus")
    private InstrumentStatusEnum instrumentStatus;

    
    @Size(max=64)
    @JsonProperty("ifscCode")
    private String ifscCode;

    @JsonProperty("auditDetails")
    private AuditDetails auditDetails;

    @JsonProperty("additionalDetails")
    private JsonNode additionalDetails;

    @JsonProperty("paymentDetails")
    @Valid
    private List<PaymentDetail> paymentDetails;

    
    @Size(max=128)
    @NotNull
    @Pattern(regexp = "^[a-zA-Z]+(([_\\-'`\\. ][a-zA-Z ])?[a-zA-Z]*)*$", message = "Invalid name. Only alphabets and special characters -, ',`, ., _")
    @JsonProperty("paidBy")
    private String paidBy = null;

    
    @Size(max=64)
    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Invalid mobile number")
    @JsonProperty("mobileNumber")
    private String mobileNumber = null;

    
    @Size(max=128)
    @Pattern(regexp = "^[a-zA-Z]+(([_\\-'`\\. ][a-zA-Z ])?[a-zA-Z]*)*$", message = "Invalid name. Only alphabets and special characters -, ',`, ., _")
    @JsonProperty("payerName")
    private String payerName = null;

    
    @Size(max=1024)
    @JsonProperty("payerAddress")
    private String payerAddress = null;

    
    @Size(max=64)
    @Pattern(regexp = "^$|^(?=^.{1,64}$)((([^<>()\\[\\]\\\\.,;:\\s$*@'\"]+(\\.[^<>()\\[\\]\\\\.,;:\\s@'\"]+)*)|(\".+\"))@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,})))$", message = "Invalid emailId")
    @JsonProperty("payerEmail")
    private String payerEmail = null;

    
    @Size(max=64)
    @JsonProperty("payerId")
    private String payerId = null;

    @JsonProperty("paymentStatus")
    private PaymentStatusEnum paymentStatus;

    
    @JsonProperty("fileStoreId")
    private String fileStoreId;


    public Payment addpaymentDetailsItem(PaymentDetail paymentDetail) {
        if (this.paymentDetails == null) {
            this.paymentDetails = new ArrayList<>();
        }
        this.paymentDetails.add(paymentDetail);
        return this;
    }




}
