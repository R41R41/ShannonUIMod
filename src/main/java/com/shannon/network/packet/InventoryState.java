package com.shannon.network.packet;

import java.util.List;

public class InventoryState {
    public List<Item> items;
    public Item mainHand;
    public Item offHand;
    public boolean isFull;
    public Item head;
    public Item chest;
    public Item legs;
    public Item feet;

    public static class Item {
        public String displayName;
        public String name;
        public String count;
    }
}
