package matt.rtask.tabular.csv

import matt.prim.str.joinWithCommas
import matt.prim.str.mybuild.lineDelimitedString
import matt.rtask.tabular.typesafe.NamedTable

fun <R, C, D> NamedTable<R, C, D>.toCsv(): String {
    val cols = this.columns()
    val colKeys = cols.map { it.key }
    return lineDelimitedString {
        +("," + colKeys.joinWithCommas())
        rows().forEach { row ->
            val rowData = row.value
            val rowKey = row.key
            +(listOf(rowKey) + colKeys.map { rowData[it] }).joinWithCommas()
        }
    }
}