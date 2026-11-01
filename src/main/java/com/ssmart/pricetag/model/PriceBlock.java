package com.ssmart.pricetag.model;

public class PriceBlock {

    private int blockNumber;
    private String price;

    public PriceBlock() {}

    public PriceBlock(int blockNumber, String price) {
        this.blockNumber = blockNumber;
        this.price = price;
    }

    public int getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(int blockNumber) {
        this.blockNumber = blockNumber;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }
}
