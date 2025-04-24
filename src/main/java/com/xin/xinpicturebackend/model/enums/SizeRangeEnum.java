package com.xin.xinpicturebackend.model.enums;

import lombok.Getter;

@Getter
public enum SizeRangeEnum {

    RANGE_1("100KB", 100 * 1024),
    RANGE_2("500KB", 500 * 1024),
    RANGE_3("1MB", 1024 * 1024);

    private final String sizeRange;
    private final int size;

    SizeRangeEnum(String sizeRange, int size) {
        this.sizeRange = sizeRange;
        this.size = size;
    }

    //通过sizeRange获取对应size
    public static int getSizeBySizeRange(String sizeRange) {
        for (SizeRangeEnum value : SizeRangeEnum.values()) {
            if (value.getSizeRange().equals(sizeRange)) {
                return value.getSize();
            }
        }
        return -1;
    }

}
