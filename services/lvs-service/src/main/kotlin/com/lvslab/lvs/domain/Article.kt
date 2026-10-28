package com.lvslab.lvs.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "article")
class Article(
    @Column(name = "article_number", nullable = false, unique = true, length = 80)
    var articleNumber: String = "",
    @Column(nullable = false, length = 200)
    var description: String = "",
    @Column(name = "storage_type", nullable = false, length = 40)
    var storageType: String = "STANDARD"
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
        protected set
}