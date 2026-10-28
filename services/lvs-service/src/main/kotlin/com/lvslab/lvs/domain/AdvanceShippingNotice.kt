package com.lvslab.lvs.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "advance_shipping_notice")
class AdvanceShippingNotice(
    @Column(name = "notice_number", nullable = false, unique = true, length = 80)
    var noticeNumber: String = "",
    @OneToMany(mappedBy = "notice", cascade = [CascadeType.ALL], orphanRemoval = true)
    var positions: MutableList<AdvanceShippingNoticePosition> = mutableListOf()
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
        protected set
}