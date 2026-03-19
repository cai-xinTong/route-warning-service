package com.warning.service;

import com.warning.entity.GeoLineNode;
import com.warning.mapper.GeoLineNodeMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ExcelImportService {

    @Resource
    private GeoLineNodeMapper geoLineNodeMapper;

    /**
     * 导入Excel数据并抽稀到3公里
     * @param excelPath Excel文件路径
     */
    public void importExcelWithDownsampling(String excelPath) {
        log.info("开始导入Excel数据: {}", excelPath);

        try (FileInputStream fis = new FileInputStream(excelPath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            int totalRows = sheet.getPhysicalNumberOfRows();
            log.info("Excel总行数: {}", totalRows);

            // 3公里 = 3000米，10米间隔，每300行取一行
            int interval = 300;

            List<GeoLineNode> nodes = new ArrayList<>();
            int seq = 0;

            for (int i = 1; i < totalRows; i++) {
                if (i % interval == 0) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    try {
                        GeoLineNode node = new GeoLineNode();
                        node.setSeq(seq++);

                        // 公司
                        Cell companyCell = row.getCell(0);
                        if (companyCell != null) {
                            node.setCompany(getCellValue(companyCell));
                        }

                        // 路段编号
                        Cell roadCodeCell = row.getCell(1);
                        if (roadCodeCell != null) {
                            node.setRoadCode(getCellValue(roadCodeCell));
                        }

                        // 路段名称
                        Cell roadNameCell = row.getCell(2);
                        if (roadNameCell != null) {
                            node.setRoadName(getCellValue(roadNameCell));
                        }

                        // 桩号
                        Cell nameCell = row.getCell(3);
                        if (nameCell != null) {
                            node.setName(getCellValue(nameCell));
                        }

                        // 经度
                        Cell lonCell = row.getCell(4);
                        if (lonCell != null) {
                            node.setLon(getNumericValue(lonCell));
                        }

                        // 纬度
                        Cell latCell = row.getCell(5);
                        if (latCell != null) {
                            node.setLat(getNumericValue(latCell));
                        }

                        nodes.add(node);

                    } catch (Exception e) {
                        log.error("解析第{}行数据失败", i, e);
                    }
                }
            }

            // 批量插入
            if (!nodes.isEmpty()) {
                // 先清空表
                geoLineNodeMapper.delete(null);
                log.info("已清空geo_line_node表");

                // 批量插入
                for (GeoLineNode node : nodes) {
                    geoLineNodeMapper.insert(node);
                }

                log.info("导入完成，共{}条记录（从{}行抽稀而来）", nodes.size(), totalRows);
            } else {
                log.warn("没有数据需要导入");
            }

        } catch (Exception e) {
            log.error("导入Excel失败", e);
            throw new RuntimeException("导入Excel失败: " + e.getMessage());
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            default:
                return cell.toString();
        }
    }

    private Double getNumericValue(Cell cell) {
        if (cell == null) return null;

        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Double.parseDouble(cell.getStringCellValue());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }
}
