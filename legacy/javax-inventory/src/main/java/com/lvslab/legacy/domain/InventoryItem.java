package com.lvslab.legacy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_item")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "article_number", nullable = false, unique = true, length = 80)
    private String articleNumber;

    @Column(nullable = false)
    private Integer quantity;

    protected InventoryItem() {}

    public InventoryItem(String articleNumber, Integer quantity) {
        this.articleNumber = articleNumber;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public String getArticleNumber() { return articleNumber; }
    public void setArticleNumber(String articleNumber) { this.articleNumber = articleNumber; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
