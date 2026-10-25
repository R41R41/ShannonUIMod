package com.shannon.client.request;

/**
 * アイテム投げ捨てリクエスト
 */
public class ThrowItemRequest {
    private String itemName;
    private int count;
    /** The player who asked, so the bot knows whom to give it to when several are on the server. */
    private String sender;

    public ThrowItemRequest(String itemName, int count, String sender) {
        this.itemName = itemName;
        this.count = count;
        this.sender = sender;
    }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
