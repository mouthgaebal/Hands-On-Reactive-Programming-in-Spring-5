package org.rpis5.chapters.chapter_06.sse;

public class StockItem {
    
    public StockItem(String id, String type) {
        this.id = id;
        this.type = type;
    }
    
    private String id;
    private String type;

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }
}
