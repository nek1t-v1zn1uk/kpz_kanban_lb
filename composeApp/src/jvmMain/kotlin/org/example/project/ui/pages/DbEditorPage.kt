package org.example.project.ui.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.example.project.data.dtos.KanbanBoardDto
import org.example.project.data.dtos.KanbanColumnDto
import org.example.project.data.dtos.KanbanTaskDto
import org.example.project.data.dtos.KanbanTaskPriorities
import org.example.project.data.dtos.ProjectDto
import org.example.project.data.dtos.ProjectMemberDto
import org.example.project.data.dtos.ProjectMemberRole
import org.example.project.data.dtos.UserDto
import org.example.project.viewmodels.DbEditorViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.Long
import kotlin.String


val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

fun DateToString(date: LocalDateTime): String {
    return date.format(formatter)
}
fun StringToDate(string: String): LocalDateTime {
    return LocalDateTime.parse(string, formatter)
}

@Composable
fun DbEditorPage(viewModel: DbEditorViewModel) {
    MaterialTheme {
        val tabs = listOf(
            TabInfo("users") { usersTab(viewModel) },
            TabInfo("projects") { projectsTab(viewModel) },
            TabInfo("project_members") { projectMembersTab(viewModel) },
            TabInfo("kanban_boards") { boardsTab(viewModel) },
            TabInfo("kanban_columns") { columnsTab(viewModel) },
            TabInfo("kanban_tasks") { tasksTab(viewModel) },
        )
        var selectedTabIndex by remember { mutableStateOf(0) }
        Column {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                        },
                        text = {
                            Text(tab.title)
                        }
                    )
                }
            }

            Column(Modifier.fillMaxSize().padding(16.dp)) {
                tabs[selectedTabIndex].content()
            }
        }
        val error by viewModel.error
        if(error != null) {
            Dialog(
                onDismissRequest = { viewModel.removeError() },
            ) {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "В ході запиту виникла помилка:",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Text(
                            error.toString(),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

data class TabInfo(val title: String, val content: @Composable () -> Unit)

data class DbTableData(val columns: List<DbColumnData>, val rows: List<DbRowData>)
data class DbColumnData(
    val name: String,
    var type: Type,
    var limitation: Int? = null,
    var changeable: Boolean = true,
    var enumOptions: List<String>? = null,
    var primaryKey: Boolean = false,
    var foreignKey: Boolean = false,
    var sort: MutableState<Sort> = mutableStateOf(Sort.None),
    var sortPriority: MutableState<Int> = mutableStateOf(0),
    var search: MutableState<String?> = mutableStateOf(null),
    var searchPriority: MutableState<Int> = mutableStateOf(0),
    var filter: MutableState<Pair<String?, Filter>> = mutableStateOf(Pair(null, Filter.None)),
    var filterPriority: MutableState<Int> = mutableStateOf(0),
) {
    companion object {
        private var LastSortPriority = 0
        private var LastSearchPriority = 0
        private var LastFilterPriority = 0
    }
    val formattedName: String
        get() {
            var res = ""
            if(primaryKey)
                res += "PK "
            else if(foreignKey)
                res += "FK "
            res += "$name - $type"
            if(type == Type.varchar)
                res += "($limitation)"


            return res
        }
    fun setSortPriority() {
        LastSortPriority++
        sortPriority.value = LastSortPriority
    }
    fun noSortPriority() {
        sortPriority.value = 0
    }
    fun setSearchPriority() {
        LastSearchPriority++
        searchPriority.value = LastSearchPriority
    }
    fun noSearchPriority() {
        searchPriority.value = 0
    }
    fun setFilterPriority() {
        LastFilterPriority++
        filterPriority.value = LastFilterPriority
    }
    fun noFilterPriority() {
        filterPriority.value = 0
    }
    enum class Type{
        varchar,
        text,
        enum,
        numeric,
        timestamp
    }
    enum class Sort {
        None,
        AZ,
        ZA
    }
    enum class Filter {
        None,
        StartsWith,
        EndsWith,
        Contains,
        GreaterThan,
        LessThen,
        Equals
    }
}

data class DbRowData(val cells: List<DbCellData>)
data class DbCellData(
    var value: String,
)

@Composable
fun DbTable(
    title: String,
    tableData: DbTableData,
    onAdd: (List<String>) -> Unit,
    onEdit: (List<String>) -> Unit,
    onDelete: (Long) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .border(1.dp, Color.Black)
            .padding(16.dp)
    ) {
        // Title
        Text(text = title)
        //Header
        val columns = remember { tableData.columns.toMutableStateList() }
        HeadRow(
            columns = columns,
            extraFunctions = true
        )
        //Rows
        val rows = remember {
            derivedStateOf {
                /*val searchList = columns.filter {
                    it.search.value != null
                }.map {
                    Triple(it.searchPriority.value, it.search.value, it.name)
                }.sortedByDescending {
                    it.first
                }
                var searchedList = tableData.rows
                for (search in searchList) {
                    val indexOfCell = columns.indexOfFirst { it.name == search.third }
                    searchedList = searchedList.filter {
                        it.cells[indexOfCell].value == search.second
                    }
                }*/



                val sortList = columns.filter {
                    it.sort.value != DbColumnData.Sort.None
                }.map {
                    Triple(it.sortPriority.value, it.sort.value, it.name)
                }.sortedByDescending {
                    it.first
                }
                var sortedList = tableData.rows
                for(sort in sortList) {
                    val indexOfCell = columns.indexOfFirst { it.name == sort.third }
                    val isNumeric = columns[indexOfCell].type == DbColumnData.Type.numeric
                    sortedList = if(sort.second == DbColumnData.Sort.AZ)
                        if(isNumeric)
                            sortedList.sortedBy { it.cells[indexOfCell].value.toInt() }
                        else
                            sortedList.sortedBy { it.cells[indexOfCell].value }
                    else
                        if(isNumeric)
                            sortedList.sortedByDescending { it.cells[indexOfCell].value.toInt() }
                        else
                            sortedList.sortedByDescending { it.cells[indexOfCell].value }
                }

                sortedList.toMutableStateList()
            }
        }.value
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, Color.Black)
        ) {
            //View Rows
            LazyColumn {
                itemsIndexed(rows) { index, row ->
                    DbRow(
                        tableData.columns,
                        row.cells,
                        onEdit,
                        onDelete
                    )
                }
            }
            //Add New Row
            val resetValues: () -> List<String> = {
                tableData.columns.withIndex().map{ (index, column) ->
                    if(column.foreignKey || column.type == DbColumnData.Type.enum){
                        if(column.enumOptions != null && column.enumOptions!!.isNotEmpty())
                            column.enumOptions!![0]
                        else
                            "None"
                    }
                    else if(column.type == DbColumnData.Type.varchar) { "" }
                    else if(column.type == DbColumnData.Type.text) { "" }
                    else if(column.type == DbColumnData.Type.numeric) { "0" }
                    else if(column.type == DbColumnData.Type.timestamp) {
                        DateToString(LocalDateTime.now())
                    }
                    else { "" }
                }
            }
            val values = remember { resetValues().toMutableStateList() }
            Column {
                Divider(Modifier.height(16.dp))
                HeadRow(columns = columns)
                RowFields(
                    tableData.columns,
                    values.map { DbCellData("") },
                    values
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        ActionButton("CREATE", Color.Green, Modifier.weight(1f)) {
                            onAdd(values)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeadRow(
    columns: SnapshotStateList<DbColumnData>,
    extraFunctions: Boolean = false,
){
    Row(
        Modifier
            .heightIn(50.dp, 100.dp)
    ) {
        for ((index, column) in columns.withIndex()) {
            Column(
                Modifier
                    .weight(1f)
                    .background(
                        if (index % 2 == 0) Color(0xFFCCCCCC)
                        else Color(0xFFE7E7E7),
                    )
            ) {
                DbCell(
                    Modifier
                        .weight(1f)
                ) {
                    if (extraFunctions) {
                        Row {
                            JustText(
                                column.formattedName, TextAlign.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )

                            Button(
                                contentPadding = PaddingValues(2.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFBBBBBB),
                                ),
                                shape = RoundedCornerShape(5.dp),
                                border = BorderStroke(1.dp, Color.DarkGray),
                                onClick = {
                                    column.sort.value = when (column.sort.value) {
                                        DbColumnData.Sort.None -> {
                                            column.setSortPriority()
                                            DbColumnData.Sort.AZ
                                        }
                                        DbColumnData.Sort.AZ -> {
                                            column.setSortPriority()
                                            DbColumnData.Sort.ZA
                                        }
                                        DbColumnData.Sort.ZA -> {
                                            column.noSortPriority()
                                            DbColumnData.Sort.None
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .padding(4.dp)
                                    .fillMaxHeight()
                                    .aspectRatio(1f)
                            ) {
                                Text(
                                    when (column.sort.value) {
                                        DbColumnData.Sort.None -> "="
                                        DbColumnData.Sort.AZ -> "↓"
                                        DbColumnData.Sort.ZA -> "↑"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.Black,
                                )
                            }
                        }
                    } else {
                        JustText(column.formattedName, TextAlign.Center)
                    }
                }
                if(extraFunctions) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .border(1.dp, Color.Black)
                            .padding(16.dp, 4.dp)
                            .weight(1f)
                    ) {
                        var isSearchDialog by remember { mutableStateOf(false) }
                        var isFilterDialog by remember { mutableStateOf(false) }

                        if(column.search.value != null) {
                            JustText("\uD83D\uDD0D ${column.search.value}", modifier = Modifier.weight(1f))
                            ActionButton(
                                "X",
                                Color(0xFFAAAAAA),
                                modifier = Modifier
                                    .padding(4.dp)
                                    .fillMaxHeight()
                                    .aspectRatio(1f)
                            ) {
                                column.search.value = null
                                column.noSearchPriority()
                            }
                        }
                        else if(column.filter.value.second != DbColumnData.Filter.None) {
                            JustText(
                                "⌛ " +
                                        when (column.filter.value.second) {
                                            DbColumnData.Filter.None -> ""
                                            DbColumnData.Filter.StartsWith -> column.filter.value.first + " ..."
                                            DbColumnData.Filter.EndsWith -> "... " + column.filter.value.first
                                            DbColumnData.Filter.Contains -> "... " + column.filter.value.first + " ..."
                                            DbColumnData.Filter.GreaterThan -> "> " + column.filter.value.first
                                            DbColumnData.Filter.LessThen -> "< " + column.filter.value.first
                                            DbColumnData.Filter.Equals -> "= " + column.filter.value.first
                                        },
                                modifier = Modifier.weight(1f)
                            )
                            ActionButton(
                                "X",
                                Color(0xFFAAAAAA),
                                modifier = Modifier
                                    .padding(4.dp)
                                    .fillMaxHeight()
                                    .aspectRatio(1f)
                            ) {
                                column.filter.value = Pair(null, DbColumnData.Filter.None)
                                column.noFilterPriority()
                            }
                        }
                        else {
                            ActionButton("Пошук", Color(0xFFAAAAAA), modifier = Modifier.weight(1f)) {
                                isSearchDialog = true
                            }
                            ActionButton("Фільтр", Color(0xFFAAAAAA), modifier = Modifier.weight(1f)) {
                                isFilterDialog = true
                            }
                        }

                        if (isSearchDialog) {
                            Dialog(
                                onDismissRequest = { isSearchDialog = false },
                            ) {
                                Card {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text("Введіть значення для пошуку:")
                                        var value by remember {
                                            mutableStateOf(
                                                if (column.type == DbColumnData.Type.timestamp) DateToString(
                                                    LocalDateTime.now()
                                                )
                                                else if (column.foreignKey || column.type == DbColumnData.Type.enum) column.enumOptions?.let { it[0] }
                                                    ?: "None"
                                                else ""
                                            )
                                        }
                                        Box(
                                            Modifier
                                                .height(50.dp)
                                        ) {
                                            if (column.type == DbColumnData.Type.timestamp) {
                                                MyDateTimePicker(
                                                    value,
                                                    { value = DateToString(it) },
                                                )
                                            } else if (column.foreignKey || column.type == DbColumnData.Type.enum) {
                                                MyDropDown(
                                                    column.enumOptions!!,
                                                    value,
                                                    { value = it }
                                                )
                                            } else {
                                                MyTextField(
                                                    value,
                                                    { value = it },
                                                    isNumeric = column.type == DbColumnData.Type.numeric,
                                                )
                                            }
                                        }
                                        Button(
                                            onClick = {
                                                column.search.value = value
                                                column.setSearchPriority()

                                                isSearchDialog = false
                                            }
                                        ) {
                                            Text("Підтвердити")
                                        }
                                    }
                                }
                            }
                        }
                        else if (isFilterDialog) {
                            Dialog(
                                onDismissRequest = { isFilterDialog = false },
                            ) {
                                Card {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text("Введіть значення для фільтрації:")
                                        var value by remember {
                                            mutableStateOf(
                                                if (column.type == DbColumnData.Type.timestamp) DateToString(
                                                    LocalDateTime.now()
                                                )
                                                else if (column.type == DbColumnData.Type.numeric) "0"
                                                else ""
                                            )
                                        }
                                        var parametr by remember {
                                            mutableStateOf(
                                                if (column.type == DbColumnData.Type.timestamp) DateToString(
                                                    LocalDateTime.now()
                                                )
                                                else if (column.type == DbColumnData.Type.numeric) ">"
                                                else "Starts with: "
                                            )
                                        }
                                        Column {
                                            if (column.type == DbColumnData.Type.timestamp) {
                                                MyDateTimePicker(
                                                    value,
                                                    { value = DateToString(it) },
                                                )
                                            } else {
                                                Box(
                                                    Modifier
                                                        .height(50.dp)
                                                ) {
                                                    MyDropDown(
                                                        if (column.type == DbColumnData.Type.numeric) listOf(">", "<", "=")
                                                        else listOf("Starts with: ", "Ends with: ", "Contains: "),
                                                        parametr,
                                                        { parametr = it },
                                                    )
                                                }
                                                Box(
                                                    Modifier
                                                        .height(50.dp)
                                                ) {
                                                    MyTextField(
                                                        value,
                                                        { value = it },
                                                        isNumeric = column.type == DbColumnData.Type.numeric,
                                                    )
                                                }
                                            }
                                        }
                                        Button(
                                            onClick = {
                                                column.filter.value = Pair(
                                                    value,
                                                    when(parametr) {
                                                        "Starts with: " -> DbColumnData.Filter.StartsWith
                                                        "Ends with: " -> DbColumnData.Filter.EndsWith
                                                        "Contains: " -> DbColumnData.Filter.Contains
                                                        ">" -> DbColumnData.Filter.GreaterThan
                                                        "<" -> DbColumnData.Filter.LessThen
                                                        "=" -> DbColumnData.Filter.Equals
                                                        else -> DbColumnData.Filter.None
                                                    }
                                                )
                                                column.noFilterPriority()

                                                isFilterDialog = false
                                            }
                                        ) {
                                            Text("Підтвердити")
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
        DbCell(
            Modifier
                .weight(1f)
                .background(Color.Cyan)
        ) {
            JustText("ACTIONS", TextAlign.Center)
        }
    }
}
@Composable
fun DbRow(
    columns: List<DbColumnData>,
    cells: List<DbCellData>,
    onEdit: (List<String>) -> Unit,
    onDelete: (Long) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    if(isEditing){
        val resetValues: () -> List<String> = {
            columns.withIndex().map{ (index, column) ->
                when(column.type){
                    DbColumnData.Type.varchar -> {
                        cells[index].value
                    }
                    DbColumnData.Type.text -> {
                        cells[index].value
                    }
                    DbColumnData.Type.numeric -> {
                        cells[index].value
                    }
                    DbColumnData.Type.enum -> {
                        cells[index].value
                    }
                    DbColumnData.Type.timestamp -> {
                        cells[index].value
                    }
                }
            }
        }
        var values = remember { resetValues().toMutableStateList() }
        RowFields(
            columns,
            cells,
            values
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ActionButton("SAVE", Color.Green, Modifier.weight(1f)) {
                    isEditing = false
                    onEdit(values)
                }
                ActionButton("CANCEL", Color.Red, Modifier.weight(1f)) {
                    isEditing = false
                    values = resetValues().toMutableStateList()
                }
            }
        }
    } else {
        RowTexts(
            cells
        ){
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ActionButton("EDIT", Color.Yellow, Modifier.weight(1f)) {
                    isEditing = true
                }
                ActionButton("DELETE", Color.Red, Modifier.weight(1f)) {
                    val pkIndex = columns.indexOf(columns.find { it.primaryKey })
                    onDelete(cells[pkIndex].value.toLong())
                }
            }
        }
    }
}

@Composable
fun RowTexts(
    cells: List<DbCellData>,
    buttons: @Composable () -> Unit
) {
    Row(
        Modifier
            .height(32.dp)
    ) {
        for ((index, cell) in cells.withIndex()) {
            DbCell(
                Modifier
                    .weight(1f)
                    .background(
                        if (index % 2 == 0) Color(0xFFCCCCCC)
                        else Color(0xFFE7E7E7),
                    )
            ) {
                JustText(cell.value)
            }
        }
        DbCell(
            Modifier
                .weight(1f)
                .background(Color.Cyan)
        ) {
            buttons()
        }
    }
}
@Composable
fun RowFields(
    columns: List<DbColumnData>,
    cells: List<DbCellData>,
    values: SnapshotStateList<String>,
    buttons: @Composable () -> Unit
) {
    Row(
        Modifier
            .height(40.dp)
    ) {
        for ((index, cell) in cells.withIndex()) {
            DbCell(
                Modifier
                    .weight(1f)
                    .background(
                        if (index % 2 == 0) Color(0xFFCCCCCC)
                        else Color(0xFFE7E7E7),
                    )
            ) {
                if(columns[index].foreignKey || columns[index].type == DbColumnData.Type.enum){
                    MyDropDown(
                        columns[index].enumOptions!!,
                        values[index],
                        { newValue ->
                            values[index] = newValue
                        }
                    )
                }
                else if(columns[index].type == DbColumnData.Type.varchar){
                    MyTextField(
                        values[index],
                        { newValue ->
                            values[index] = newValue
                        },
                        maxLength = columns[index].limitation,
                        enabled = columns[index].changeable
                    )
                }
                else if(columns[index].type == DbColumnData.Type.text){
                    MyTextField(
                        values[index],
                        { newValue ->
                            values[index] = newValue
                        },
                        enabled = columns[index].changeable
                    )
                }
                else if(columns[index].type == DbColumnData.Type.numeric){
                    MyTextField(
                        values[index],
                        { newValue ->
                            values[index] = newValue
                        },
                        isNumeric = true,
                        enabled = columns[index].changeable
                    )
                }
                else if(columns[index].type == DbColumnData.Type.timestamp){
                    MyDateTimePicker(
                        values[index],
                        { newValue -> values[index] = DateToString(newValue) },
                        enabled = columns[index].changeable
                    )
                }
            }
        }
        DbCell(
            Modifier
                .weight(1f)
                .background(Color.Cyan)
        ) {
            buttons()
        }
    }
}

@Composable
fun DbCell(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .fillMaxHeight()
            .border(1.dp, Color.Black)
            .padding(8.dp, 0.dp)
    ) {
        content()
    }
}

@Composable
fun JustText(
    text: String,
    textAlign: TextAlign = TextAlign.Left,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Text(
            text = text,
            textAlign = textAlign,
            fontSize = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}
@Composable
fun ActionButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    action: () -> Unit
){
    Button(
        colors = ButtonDefaults.buttonColors(
            contentColor = Color.Black,
            containerColor = color
        ),
        contentPadding = PaddingValues(0.dp),
        onClick = action,
        modifier = modifier
    ) {
        Text(text)
    }
}
@Composable
fun MyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    isNumeric: Boolean = false,
    maxLength: Int? = null,
    enabled: Boolean = true,
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(text = value)) }
    BasicTextField(
        value = textFieldValue,
        onValueChange = { originNewValue ->
            var newTextValue = originNewValue.text
            var newCursorIndex = originNewValue.selection.start
            if(maxLength == null || newTextValue.length <= maxLength)
                if(isNumeric) {
                    val filteredTextValue = newTextValue.filter { it.isDigit() }
                    newCursorIndex -= newTextValue.length - filteredTextValue.length //зменшити позицію на к-сть НЕ-цифр
                    newTextValue = filteredTextValue
                }

            textFieldValue = TextFieldValue(
                text = newTextValue,
                selection = TextRange(newCursorIndex)
            )
            onValueChange(textFieldValue.text)
        },
        textStyle = TextStyle(color = Color.Black, fontSize = 16.sp),
        enabled = enabled,
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp, 4.dp)
            .wrapContentHeight(Alignment.CenterVertically)
            .background(
                if(enabled) Color.White
                else Color(0xFFAAAAAA),
                RoundedCornerShape(8.dp))
            .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
            .padding(8.dp, 2.dp)
    )
}
@Composable
fun MyDropDown(
    options: List<String>,
    value: String,
    onValueChange: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        Modifier
            .padding(0.dp, 6.dp)
            .background(Color.LightGray, RoundedCornerShape(8.dp))
            .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    isExpanded = !isExpanded
                }
                .wrapContentHeight(Alignment.CenterVertically)
                .padding(8.dp, 2.dp)
        )

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = {
                isExpanded = false
            },
        ) {
            for (option in options) {
                DropdownMenuItem(
                    onClick = {
                        onValueChange(option)
                        isExpanded = false
                    }
                ) {
                    Text(option)
                }
            }
        }
    }
}
@Composable
fun MyDateTimePicker(
    stringDate: String,
    onValueChange: (LocalDateTime) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var currentDateTime by remember { mutableStateOf(StringToDate(stringDate)) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var tempDate by remember { mutableStateOf<LocalDate?>(null) }

    OutlinedButton(
        onClick = { showDatePicker = true },
        colors = ButtonDefaults.buttonColors(
            containerColor =
                if(enabled) Color.White
                else Color(0xFFAAAAAA),
            contentColor = Color.Black,
        ),
        contentPadding = PaddingValues(0.dp),
        enabled = enabled,
        modifier = modifier
            .padding(0.dp, 2.dp)
            .fillMaxSize()
            .wrapContentHeight(Alignment.CenterVertically)
    ) {
        Text(
            DateToString(currentDateTime),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
        )
    }

    if (showDatePicker) {
        var dateString by remember { mutableStateOf(DateToString(currentDateTime).substring(0, 10)) }
        var isError by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showDatePicker = false }) {
            Card(modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Введіть Дату (YYYY-MM-DD)")
                    OutlinedTextField(value = dateString, onValueChange = { dateString = it; isError = false }, isError = isError)

                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        try {
                            tempDate = LocalDate.parse(dateString)
                            showDatePicker = false
                            showTimePicker = true
                        } catch (e: DateTimeParseException) {
                            isError = true
                        }
                    }) { Text("Далі (Час)") }
                }
            }
        }
    }

    if (showTimePicker) {
        var timeString by remember { mutableStateOf(DateToString(currentDateTime).substring(11, 16)) }
        var isError by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showTimePicker = false }) {
            Card(modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Введіть Час (HH:MM)")
                    OutlinedTextField(value = timeString, onValueChange = { timeString = it; isError = false }, isError = isError)

                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        try {
                            val time = LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm"))
                            val newDateTime = tempDate!!.atTime(time)

                            currentDateTime = newDateTime
                            onValueChange(newDateTime)
                            showTimePicker = false

                            tempDate = null
                        } catch (e: Exception) {
                            isError = true
                        }
                    }) { Text("Підтвердити") }
                }
            }
        }
    }

}


@Composable
fun usersTab(viewModel: DbEditorViewModel) {
    val users by viewModel.users
    val isLoading by viewModel.isLoading

    LaunchedEffect(Unit) {
        viewModel.getAllUsers()
    }

    if (isLoading) {
        CircularProgressIndicator()
    } else {
        DbTable(
            "Користувачі",
            DbTableData(
                listOf(
                    DbColumnData(
                        "id",
                        DbColumnData.Type.numeric,
                        changeable = false,
                        primaryKey = true
                    ),
                    DbColumnData(
                        "username",
                        DbColumnData.Type.varchar,
                        32
                    ),
                    DbColumnData(
                        "email",
                        DbColumnData.Type.text
                    ),
                    DbColumnData(
                        "password_hash",
                        DbColumnData.Type.text
                    )
                ),
                users.map {
                    DbRowData(
                        listOf(
                            DbCellData(
                                it.id.toString()
                            ),
                            DbCellData(
                                it.username.toString()
                            ),
                            DbCellData(
                                it.email
                            ),
                            DbCellData(
                                it.passwordHash
                            )
                        )
                    )
                }
            ),
            onAdd = { values: List<String> ->
                viewModel.addUser(
                    UserDto(
                        id = null,
                        username = values[1],
                        email = values[2],
                        passwordHash = values[3]
                    )
                )
            },
            onEdit = { values: List<String> ->
                viewModel.editUser(
                    UserDto(
                        id = values[0].toLong(),
                        username = values[1],
                        email = values[2],
                        passwordHash = values[3]
                    )
                )
            },
            onDelete = { id: Long ->
                viewModel.deleteUser(id)
            },
        )
    }
}

@Composable
fun projectsTab(viewModel: DbEditorViewModel) {
    val users by viewModel.users
    val projects by viewModel.projects
    val isLoading by viewModel.isLoading

    LaunchedEffect(Unit) {
        viewModel.getAllUsers()
        viewModel.getAllProjects()
    }

    if (isLoading) {
        CircularProgressIndicator()
    } else {
        DbTable(
            "Проєкти",
            DbTableData(
                listOf(
                    DbColumnData(
                        "id",
                        DbColumnData.Type.numeric,
                        changeable = false,
                        primaryKey = true
                    ),
                    DbColumnData(
                        "title",
                        DbColumnData.Type.varchar,
                        255
                    ),
                    DbColumnData(
                        "description",
                        DbColumnData.Type.text
                    ),
                    DbColumnData(
                        "created_at",
                        DbColumnData.Type.timestamp,
                        changeable = false
                    ),
                    DbColumnData(
                        "updated_at",
                        DbColumnData.Type.timestamp,
                        changeable = false
                    ),
                    DbColumnData(
                        "owner_id",
                        DbColumnData.Type.numeric,
                        enumOptions = users.map{ it.id.toString() },
                        foreignKey = true
                    )
                ),
                projects.map {
                    DbRowData(
                        listOf(
                            DbCellData(
                                it.id.toString()
                            ),
                            DbCellData(
                                it.title
                            ),
                            DbCellData(
                                it.description.toString()
                            ),
                            DbCellData(
                                DateToString(it.createdAt!!)
                            ),
                            DbCellData(
                                DateToString(it.updatedAt!!)
                            ),
                            DbCellData(
                                it.ownerId.toString()
                            )
                        )
                    )
                }
            ),
            onAdd = { values: List<String> ->
                viewModel.addProject(
                    ProjectDto(
                        id = null,
                        title = values[1],
                        description = values[2],
                        createdAt = null,
                        updatedAt = null,
                        ownerId = values[5].toLong(),
                    )
                )
            },
            onEdit = { values: List<String> ->
                viewModel.editProject(
                    ProjectDto(
                        id = values[0].toLong(),
                        title = values[1],
                        description = values[2],
                        createdAt = null,
                        updatedAt = null,
                        ownerId = values[5].toLong(),
                    )
                )
            },
            onDelete = { id: Long ->
                viewModel.deleteProject(id)
            },
        )
    }
}

@Composable
fun projectMembersTab(viewModel: DbEditorViewModel) {
    val users by viewModel.users
    val projects by viewModel.projects
    val projectMembers by viewModel.projectMembers
    val isLoading by viewModel.isLoading

    LaunchedEffect(Unit) {
        viewModel.getAllUsers()
        viewModel.getAllProjects()
        viewModel.getAllProjectMembers()
    }

    if (isLoading) {
        CircularProgressIndicator()
    } else {
        DbTable(
            "Члени проєкту",
            DbTableData(
                listOf(
                    DbColumnData(
                        "id",
                        DbColumnData.Type.numeric,
                        changeable = false,
                        primaryKey = true
                    ),
                    DbColumnData(
                        "role",
                        DbColumnData.Type.enum,
                        enumOptions = ProjectMemberRole.entries.map { it.name }
                    ),
                    DbColumnData(
                        "project_id",
                        DbColumnData.Type.numeric,
                        enumOptions = projects.map{ it.id.toString() },
                        foreignKey = true
                    ),
                    DbColumnData(
                        "user_id",
                        DbColumnData.Type.numeric,
                        enumOptions = users.map{ it.id.toString() },
                        foreignKey = true
                    )
                ),
                projectMembers.map {
                    DbRowData(
                        listOf(
                            DbCellData(
                                it.id.toString()
                            ),
                            DbCellData(
                                it.role.toString()
                            ),
                            DbCellData(
                                it.projectId.toString()
                            ),
                            DbCellData(
                                it.userId.toString()
                            )
                        )
                    )
                }
            ),
            onAdd = { values: List<String> ->
                viewModel.addProjectMember(
                    ProjectMemberDto(
                        id = null,
                        role = ProjectMemberRole.valueOf(values[1]),
                        projectId = values[2].toLong(),
                        userId = values[3].toLong()
                    )
                )
            },
            onEdit = { values: List<String> ->
                viewModel.editProjectMember(
                    ProjectMemberDto(
                        id = values[0].toLong(),
                        role = ProjectMemberRole.valueOf(values[1]),
                        projectId = values[2].toLong(),
                        userId = values[3].toLong()
                    )
                )
            },
            onDelete = { id: Long ->
                viewModel.deleteProjectMember(id)
            },
        )
    }
}

@Composable
fun boardsTab(viewModel: DbEditorViewModel) {
    val projects by viewModel.projects
    val boards by viewModel.kanbanBoards
    val isLoading by viewModel.isLoading

    LaunchedEffect(Unit) {
        viewModel.getAllProjects()
        viewModel.getAllKanbanBoards()
    }

    if (isLoading) {
        CircularProgressIndicator()
    } else {
        DbTable(
            "Дошки",
            DbTableData(
                listOf(
                    DbColumnData(
                        "id",
                        DbColumnData.Type.numeric,
                        changeable = false,
                        primaryKey = true
                    ),
                    DbColumnData(
                        "title",
                        DbColumnData.Type.varchar,
                        limitation = 255
                    ),
                    DbColumnData(
                        "description",
                        DbColumnData.Type.text
                    ),
                    DbColumnData(
                        "created_at",
                        DbColumnData.Type.timestamp,
                        changeable = false
                    ),
                    DbColumnData(
                        "updated_at",
                        DbColumnData.Type.timestamp,
                        changeable = false
                    ),
                    DbColumnData(
                        "project_id",
                        DbColumnData.Type.numeric,
                        enumOptions = projects.map{ it.id.toString() },
                        foreignKey = true
                    )
                ),
                boards.map {
                    DbRowData(
                        listOf(
                            DbCellData(
                                it.id.toString()
                            ),
                            DbCellData(
                                it.title
                            ),
                            DbCellData(
                                it.description.toString()
                            ),
                            DbCellData(
                                DateToString(it.createdAt!!)
                            ),
                            DbCellData(
                                DateToString(it.updatedAt!!)
                            ),
                            DbCellData(
                                it.projectId.toString()
                            )
                        )
                    )
                }
            ),
            onAdd = { values: List<String> ->
                viewModel.addKanbanBoard(
                    KanbanBoardDto(
                        id = null,
                        title = values[1],
                        description = values[2],
                        createdAt = null,
                        updatedAt = null,
                        projectId = try {
                            values[5].toLong()
                        } catch (e: Exception) {
                            1
                        },
                    )
                )
            },
            onEdit = { values: List<String> ->
                viewModel.editKanbanBoard(
                    KanbanBoardDto(
                        id = values[0].toLong(),
                        title = values[1],
                        description = values[2],
                        createdAt = null,
                        updatedAt = null,
                        projectId = values[5].toLong(),
                    )
                )
            },
            onDelete = { id ->
                viewModel.deleteKanbanBoard(id)
            }
        )
    }
}

@Composable
fun columnsTab(viewModel: DbEditorViewModel) {
    val columns by viewModel.kanbanColumns
    val boards by viewModel.kanbanBoards
    val isLoading by viewModel.isLoading

    LaunchedEffect(Unit) {
        viewModel.getAllKanbanColumns()
        viewModel.getAllKanbanBoards()
    }

    if (isLoading) {
        CircularProgressIndicator()
    } else {
        DbTable(
            "Стовпці",
            DbTableData(
                listOf(
                    DbColumnData(
                        "id",
                        DbColumnData.Type.numeric,
                        changeable = false,
                        primaryKey = true
                    ),
                    DbColumnData(
                        "title",
                        DbColumnData.Type.varchar,
                        limitation = 255
                    ),
                    DbColumnData(
                        "position",
                        DbColumnData.Type.numeric
                    ),
                    DbColumnData(
                        "created_at",
                        DbColumnData.Type.timestamp,
                        changeable = false
                    ),
                    DbColumnData(
                        "updated_at",
                        DbColumnData.Type.timestamp,
                        changeable = false
                    ),
                    DbColumnData(
                        "board_id",
                        DbColumnData.Type.numeric,
                        enumOptions = boards.map{ it.id.toString() },
                        foreignKey = true
                    )
                ),
                columns.map {
                    DbRowData(
                        listOf(
                            DbCellData(
                                it.id.toString()
                            ),
                            DbCellData(
                                it.title
                            ),
                            DbCellData(
                                it.position.toString()
                            ),
                            DbCellData(
                                DateToString(it.createdAt!!)
                            ),
                            DbCellData(
                                DateToString(it.updatedAt!!)
                            ),
                            DbCellData(
                                it.boardId.toString()
                            )
                        )
                    )
                }
            ),
            onAdd = { values: List<String> ->
                viewModel.addKanbanColumn(
                    KanbanColumnDto(
                        id = null,
                        title = values[1],
                        position = try {
                            values[2].toInt()
                        } catch (e: Exception){
                            1
                        },
                        createdAt = null,
                        updatedAt = null,
                        boardId = try {
                            values[5].toLong()
                        } catch (e: Exception) {
                            1
                        },
                    )
                )
            },
            onEdit = { values: List<String> ->
                viewModel.editKanbanColumn(
                    KanbanColumnDto(
                        id = values[0].toLong(),
                        title = values[1],
                        position = try {
                            values[2].toInt()
                        } catch (e: Exception){
                            1
                        },
                        createdAt = null,
                        updatedAt = null,
                        boardId = try {
                            values[5].toLong()
                        } catch (e: Exception) {
                            1
                        },
                    )
                )
            },
            onDelete = { id ->
                viewModel.deleteKanbanColumn(id)
            }
        )
    }
}

@Composable
fun tasksTab(viewModel: DbEditorViewModel) {
    val columns by viewModel.kanbanColumns
    val tasks by viewModel.kanbanTasks
    val isLoading by viewModel.isLoading

    LaunchedEffect(Unit) {
        viewModel.getAllKanbanColumns()
        viewModel.getAllKanbanTasks()
    }

    if (isLoading) {
        CircularProgressIndicator()
    } else {
        DbTable(
            "Завдання",
            DbTableData(
                listOf(
                    DbColumnData(
                        "id",
                        DbColumnData.Type.numeric,
                        changeable = false,
                        primaryKey = true
                    ),
                    DbColumnData(
                        "title",
                        DbColumnData.Type.varchar,
                        limitation = 255
                    ),
                    DbColumnData(
                        "description",
                        DbColumnData.Type.text
                    ),
                    DbColumnData(
                        "position",
                        DbColumnData.Type.numeric
                    ),
                    DbColumnData(
                        "priority",
                        DbColumnData.Type.enum,
                        enumOptions = KanbanTaskPriorities.entries.map { it.name },
                    ),
                    DbColumnData(
                        "due_date",
                        DbColumnData.Type.timestamp
                    ),
                    DbColumnData(
                        "created_at",
                        DbColumnData.Type.timestamp,
                        changeable = false
                    ),
                    DbColumnData(
                        "updated_at",
                        DbColumnData.Type.timestamp,
                        changeable = false
                    ),
                    DbColumnData(
                        "column_id",
                        DbColumnData.Type.numeric,
                        enumOptions = columns.map{ it.id.toString() },
                        foreignKey = true
                    )
                ),
                tasks.map {
                    DbRowData(
                        listOf(
                            DbCellData(
                                it.id.toString()
                            ),
                            DbCellData(
                                it.title
                            ),
                            DbCellData(
                                it.description.toString()
                            ),
                            DbCellData(
                                it.position.toString()
                            ),
                            DbCellData(
                                it.priority.toString()
                            ),
                            DbCellData(
                                DateToString(it.dueDate!!)
                            ),
                            DbCellData(
                                DateToString(it.createdAt!!)
                            ),
                            DbCellData(
                                DateToString(it.updatedAt!!)
                            ),
                            DbCellData(
                                it.columnId.toString()
                            )
                        )
                    )
                }
            ),
            onAdd = { values: List<String> ->
                viewModel.addKanbanTask(
                    KanbanTaskDto(
                        id = null,
                        title = values[1],
                        description = values[2],
                        position = values[3].toInt(),
                        priority = KanbanTaskPriorities.valueOf(values[4]),
                        dueDate = StringToDate(values[5]),
                        createdAt = null,
                        updatedAt = null,
                        columnId = try {
                            values[8].toLong()
                        } catch (e: Exception) {
                            1
                        },
                    )
                )
            },
            onEdit = { values: List<String> ->
                viewModel.editKanbanTask(
                    KanbanTaskDto(
                        id = values[0].toLong(),
                        title = values[1],
                        description = values[2],
                        position = values[3].toInt(),
                        priority = KanbanTaskPriorities.valueOf(values[4]),
                        dueDate = StringToDate(values[5]),
                        createdAt = null,
                        updatedAt = null,
                        columnId = values[8].toLong(),
                    )
                )
            },
            onDelete = { id ->
                viewModel.deleteKanbanTask(id)
            }
        )
    }
}
