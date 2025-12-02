package com.shannon.client.request;

/**
 * アイテム投げ捨てリクエスト
 */
public class ThrowItemRequest {
    private String itemName;

    public ThrowItemRequest(String itemName) {
        this.itemName = itemName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
}
