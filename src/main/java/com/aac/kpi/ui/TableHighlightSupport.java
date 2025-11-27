package com.aac.kpi.ui;

import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

public final class TableHighlightSupport {
    private TableHighlightSupport() {}

    /** Assigns a row factory that paints rows contained in any of {@code highlightSets} with a light pink fill. */
    @SafeVarargs
    public static <T> void install(TableView<T> table, Set<T>... highlightSets) {
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (Arrays.stream(highlightSets).anyMatch(set -> set != null && set.contains(item))) {
                    setStyle("-fx-background-color: #ffe4e1;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    /** Highlights the supplied collection into the given set, clearing previous entries. */
    public static <T> void replace(TableView<T> table, Collection<T> items, Set<T> targetSet) {
        targetSet.clear();
        if (items != null && !items.isEmpty()) {
            targetSet.addAll(items);
        }
        refresh(table);
    }

    /** Adds a single item to the highlight set while keeping existing entries. */
    public static <T> void add(TableView<T> table, T item, Set<T> highlightSet) {
        if (item == null) return;
        highlightSet.add(item);
        refresh(table);
    }

    /** Clears any highlighted rows in the set. */
    public static <T> void clear(TableView<T> table, Set<T> highlightSet) {
        highlightSet.clear();
        refresh(table);
    }

    private static <T> void refresh(TableView<T> table) {
        if (table != null) table.refresh();
    }
}
