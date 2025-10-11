package io.cucumber.usageformatter;

import java.util.StringJoiner;

import static java.lang.System.lineSeparator;

final class TableFormatter {
    
    static String format(Table table, boolean[] leftAlignColumn) {
        StringJoiner joiner = new StringJoiner(lineSeparator(), lineSeparator(), lineSeparator());
        int[] longestCellLengthInColumn = findLongestCellLengthInColumn(table);
        for (String[] row : table.getRows()) {
            StringJoiner rowJoiner = new StringJoiner(" ");
            for (int j = 0; j < row.length; j++) {
                String newElement = renderCellWithPadding(
                        longestCellLengthInColumn[j],
                        row[j],
                        leftAlignColumn[j]
                );
                rowJoiner.add(newElement);
            }
            joiner.add(rowJoiner.toString());
        }
        return joiner.toString();
    }

    private static int[] findLongestCellLengthInColumn(Table renderedCells) {
        int width = renderedCells.width();
        int[] longestCellInColumnLength = new int[width];
        for (String[] row : renderedCells.getRows()) {
            for (int colIndex = 0; colIndex < width; colIndex++) {
                int current = longestCellInColumnLength[colIndex];
                int candidate = row[colIndex].length();
                longestCellInColumnLength[colIndex] = Math.max(current, candidate);
            }
        }
        return longestCellInColumnLength;
    }

    private static String renderCellWithPadding(int width, String cell, boolean leftAlign) {
        StringBuilder result = new StringBuilder();
        if (leftAlign) {
            result.append(cell);
            padSpace(result, width - cell.length());
        } else {
            padSpace(result, width - cell.length());
            result.append(cell);
        }
        return result.toString();
    }

    private static void padSpace(StringBuilder result, int padding) {
        for (int i = 0; i < padding; i++) {
            result.append(" ");
        }
    }
}
