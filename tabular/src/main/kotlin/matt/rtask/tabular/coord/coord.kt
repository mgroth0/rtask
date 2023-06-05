package matt.rtask.tabular.coord

data class CellCoordinates<out R, out C>(
    val rowId: R,
    val colId: C
)
