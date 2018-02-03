package cn.edu.ruc.iir.pixels.core.reader;

import cn.edu.ruc.iir.pixels.core.PixelsPredicate;

import java.util.Optional;

/**
 * pixels
 *
 * @author guodong
 */
public class PixelsReaderOption
{
    private String[] includedCols = new String[0];
    private PixelsPredicate predicate = null;
    private boolean skipCorruptRecords = false;
    private boolean tolerantSchemaEvolution = true;    // this may lead to column missing due to schema evolution
    private long offset = 0;
    private long length = Long.MAX_VALUE;

    public PixelsReaderOption()
    {}

    public void includeCols(String[] columnNames)
    {
        this.includedCols = columnNames;
    }

    public String[] getIncludedCols()
    {
        return includedCols;
    }

    public void predicate(PixelsPredicate predicate)
    {
        this.predicate = predicate;
    }

    public Optional<PixelsPredicate> getPredicate()
    {
        if (predicate == null) {
            return Optional.empty();
        }
        return Optional.of(predicate);
    }

    public void skipCorruptRecords(boolean skipCorruptRecords)
    {
        this.skipCorruptRecords = skipCorruptRecords;
    }

    public boolean isSkipCorruptRecords()
    {
        return skipCorruptRecords;
    }

    public void range(long offset, long length)
    {
        this.offset = offset;
        this.length = length;
    }

    public long getOffset()
    {
        return offset;
    }

    public long getLength()
    {
        return length;
    }

    public void tolerantSchemaEvolution(boolean tolerantSchemaEvolution)
    {
        this.tolerantSchemaEvolution = tolerantSchemaEvolution;
    }

    public boolean isTolerantSchemaEvolution()
    {
        return tolerantSchemaEvolution;
    }
}
