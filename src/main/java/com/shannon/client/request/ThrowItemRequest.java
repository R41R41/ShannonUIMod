package com.shannon.client.request;

/**
 * アイテム投げ捨てリクエスト
 */
public class ThrowItemRequest {
    private String itemName;
    private int count;

    public ThrowItemRequest(String itemName, int count) {
        this.itemName = itemName;
        this.count = count;
    }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
