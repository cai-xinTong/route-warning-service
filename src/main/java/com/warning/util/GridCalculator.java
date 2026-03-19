package com.warning.util;

import com.warning.dto.GridDataSet;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GridCalculator {

    /**
     * 根据经纬度计算网格索引
     */
    public static int calculateGridIndex(double lon, double lat, GridDataSet grid) {
        if (grid == null || grid.getDataArray() == null) {
            return -1;
        }

        // 计算在网格中的列索引（经度方向）
        int colIndex = (int) Math.round((lon - grid.getMinLon()) / grid.getLonGridSpace());
        // 计算在网格中的行索引（纬度方向）
        int rowIndex = (int) Math.round((lat - grid.getMinLat()) / grid.getLatGridSpace());

        // 边界检查
        if (colIndex < 0 || colIndex >= grid.getLonGridNumber() ||
            rowIndex < 0 || rowIndex >= grid.getLatGridNumber()) {
            log.debug("经纬度({}, {})超出网格范围", lon, lat);
            return -1;
        }

        // 一维数组索引 = 行索引 * 列数 + 列索引
        return rowIndex * grid.getLonGridNumber() + colIndex;
    }

    /**
     * 获取网格点的值
     */
    public static Double getGridValue(int index, GridDataSet grid) {
        if (index < 0 || grid == null || grid.getDataArray() == null) {
            return null;
        }

        if (index >= grid.getDataArray().size()) {
            return null;
        }

        Double value = grid.getDataArray().get(index);

        // 过滤无效值
        if (value == null || value.equals(grid.getNoDataValue())) {
            return null;
        }

        return value;
    }

    /**
     * 根据经纬度直接获取网格值
     */
    public static Double getGridValueByLonLat(double lon, double lat, GridDataSet grid) {
        int index = calculateGridIndex(lon, lat, grid);
        return getGridValue(index, grid);
    }
}
