package io.cucumber.usageformatter;

import java.util.ArrayList;
import java.util.List;

final class Table {

    private final List<String[]> rows = new ArrayList<>();

    Table() {
    }

    Table(String... headers) {
        this.rows.add(headers);
    }

    void add(String... row) {
        this.rows.add(row);
    }


    void addAll(List<String[]> rows) {
        this.rows.addAll(rows);
    }

    List<String[]> getRows() {
        return rows;
    }

    int width() {
        // assumed to be square and non-sparse.
        return getRows().get(0).length;
    }
    
    static Table addTo(Table a, Table b) {
        a.addAll(b.rows);
        return a;
    }


}
