package cn.edu.ruc.iir.pixels.test;

import cn.edu.ruc.iir.pixels.common.exception.MetadataException;
import cn.edu.ruc.iir.pixels.common.metadata.MetadataService;
import cn.edu.ruc.iir.pixels.common.metadata.domain.Column;
import cn.edu.ruc.iir.pixels.common.metadata.domain.Compact;
import cn.edu.ruc.iir.pixels.common.metadata.domain.Layout;
import cn.edu.ruc.iir.pixels.common.metadata.domain.Order;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * pixels
 *
 * @author guodong
 */
public class WorkloadAnalyzer
{
    private String workloadPath = "/Users/Jelly/Desktop/pixels/experiments/1187_dedup_query.txt";
    private String workloadColsLog = "/Users/Jelly/Desktop/pixels/cache/workload_cols.csv";
    private String workloadCacheLog = "/Users/Jelly/Desktop/pixels/cache/workload_cache.csv";

    /**
     * Analyze workload file.
     * Report column visit frequency and cache status
     * */
    @Test
    private void analyze()
    {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(workloadPath));
            Map<String, Integer> colCounts = new HashMap<>();
            Map<String, Integer> colAppearCounts = new HashMap<>();
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (false == line.startsWith("SELECT"))
                {
                    continue;
                }
                String[] lineSplits = line.split("\\s");
                String selection = lineSplits[1];
                String orderBy = lineSplits[6];
                String[] selectionCols = selection.split(",");
                String[] orderCols = orderBy.split(",");
                String[] cols = new String[selectionCols.length + orderCols.length];
                System.arraycopy(selectionCols, 0, cols, 0, selectionCols.length);
                System.arraycopy(orderCols, 0, cols, selectionCols.length, orderCols.length);
                for (String col : cols)
                {
                    col = col.toLowerCase();
                    if (false == colCounts.containsKey(col))
                    {
                        colCounts.put(col, 1);
                    }
                    else
                    {
                        int prevCount = colCounts.get(col);
                        colCounts.put(col, prevCount + 1);
                    }
                }
                Set<String> colsSet = new HashSet<>(Arrays.asList(cols));
                for (String col : colsSet)
                {
                    col = col.toLowerCase();
                    if (false == colAppearCounts.containsKey(col))
                    {
                        colAppearCounts.put(col, 1);
                    }
                    else
                    {
                        int prevCount = colAppearCounts.get(col);
                        colAppearCounts.put(col, prevCount + 1);
                    }
                }
            }
            reader.close();

            MetadataService metadataService = new MetadataService("dbiir01", 18888);
            Layout layout = metadataService.getLayout("pixels", "test_1187", 3).get(0);
            Compact compact = layout.getCompactObject();
            int cacheBorder = compact.getCacheBorder();
            List<String> cacheColumnlets = compact.getColumnletOrder().subList(0, cacheBorder);
            Set<String> cacheColOrderIdSet = new HashSet<>();
            for (String c : cacheColumnlets)
            {
                cacheColOrderIdSet.add(c.split(":")[1]);
            }
            Order order = layout.getOrderObject();
            List<String> columnOrder = order.getColumnOrder();
            List<String> cachedColumns = new ArrayList<>();
            for (String cid : cacheColOrderIdSet)
            {
                int id = Integer.parseInt(cid);
                cachedColumns.add(columnOrder.get(id));
            }

            // all columns
            List<Column> columns = metadataService.getColumns("pixels", "test_1187");
            Map<String, Double> columnSizes = new HashMap<>();
            for (Column column : columns)
            {
                columnSizes.put(column.getName(), column.getSize());
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(workloadColsLog));
            writer.write("colName,colCount,colAppear,colSize");
            writer.newLine();
            writer.flush();
            for (String col : colCounts.keySet())
            {
                writer.write(col + "," + colCounts.get(col) + "," + colAppearCounts.get(col) + "," + columnSizes.get(col));
                writer.newLine();
            }
            writer.close();

            // cached columns
            writer = new BufferedWriter(new FileWriter(workloadCacheLog));
            writer.write("colName,colCount,colAppear,colSize");
            writer.newLine();
            writer.flush();
            for (String col : cachedColumns)
            {
                writer.write(col + "," + (colCounts.get(col) == null ? 0 : colCounts.get(col)) + ","
                             + (colAppearCounts.get(col) == null ? 0 : colAppearCounts.get(col)) + "," + columnSizes.get(col));
                writer.newLine();
            }
            writer.close();
        }
        catch (IOException | MetadataException e) {
            e.printStackTrace();
        }
    }
}