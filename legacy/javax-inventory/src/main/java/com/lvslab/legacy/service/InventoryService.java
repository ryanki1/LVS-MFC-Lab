package com.lvslab.legacy.service;

import com.lvslab.legacy.domain.InventoryItem;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class InventoryService {

    @PersistenceContext(unitName = "InventoryPU")
    private EntityManager em;

    public List<InventoryItem> findAll() {
        return em.createQuery("SELECT i FROM InventoryItem i", InventoryItem.class)
                 .getResultList();
    }

    public InventoryItem findByArticleNumber(String articleNumber) {
        return em.createQuery(
                "SELECT i FROM InventoryItem i WHERE i.articleNumber = :nr",
                InventoryItem.class)
            .setParameter("nr", articleNumber)
            .getResultStream()
            .findFirst()
            .orElse(null);
    }

    public InventoryItem save(InventoryItem item) {
        em.persist(item);
        return item;
    }
}
