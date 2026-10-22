package com.lvslab.lvs.domain

sealed class LvsException(message: String) : RuntimeException(message)

class NoAvailableStorageLocation(type: String) :
    LvsException("No free storage location is available for type '$type'.")

class InvalidGoodsReceipt(message: String) : LvsException(message)

class ResourceNotFound(resource: String, id: Any) :
    LvsException("$resource with id '$id' was not found.")