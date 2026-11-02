package fu.se.badmintonshop.modules.invoice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import fu.se.badmintonshop.modules.order.entity.Order;

@Entity
@Table(name = "invoices", uniqueConstraints = @UniqueConstraint(columnNames = "invoice_no"), indexes = @Index(name = "idx_inv_order", columnList = "order_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_no", nullable = false, length = 30)
    private String invoiceNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "invoice_type", nullable = false, length = 20)
    @Builder.Default
    private String invoiceType = "sale";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_invoice_id", nullable = true)
    private Invoice parentInvoice;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "issued";

    @Nationalized
    @Column(name = "buyer_name", length = 150)
    private String buyerName;

    @Column(name = "buyer_email", length = 190)
    private String buyerEmail;

    @Nationalized
    @Column(name = "buyer_address", length = 500)
    private String buyerAddress;

    @Column(name = "buyer_tax_code", length = 30)
    private String buyerTaxCode;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 0)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal totalAmount;

    @Nationalized
    @Column(name = "reason", length = 300)
    private String reason;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @Column(name = "issued_by")
    private Long issuedBy;

    @CreationTimestamp
    @Column(name = "issued_at", nullable = false, columnDefinition = "datetime2(0)")
    private LocalDateTime issuedAt;

    @Builder.Default
    @OneToMany(mappedBy = "parentInvoice", fetch = FetchType.LAZY)
    private List<Invoice> adjustments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY)
    private List<InvoiceItem> items = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY)
    private List<InvoiceDelivery> deliveries = new ArrayList<>();
}
