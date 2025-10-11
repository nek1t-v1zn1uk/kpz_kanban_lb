package org.example.project.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.toLocalDate
import kotlinx.serialization.Serializable
import org.example.project.data.dtos.KanbanBoardDto
import org.example.project.data.dtos.KanbanColumnDto
import org.example.project.data.dtos.KanbanTaskDto
import org.example.project.data.dtos.KanbanTaskPriorities
import org.example.project.data.dtos.ProjectDto
import org.example.project.data.dtos.ProjectMemberDto
import org.example.project.data.dtos.ProjectMemberRole
import org.example.project.data.dtos.UserDto
import org.example.project.utils.LocalDateTimeSerializer
import org.example.project.viewmodels.DbEditorViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.Long
import kotlin.String


val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

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
    }
}

data class TabInfo(val title: String, val content: @Composable () -> Unit)

data class DbColumns(val columns: List<Map<String, *>>)
data class DbRows(val rows: List<Map<String, *>>)

@Composable
fun DbTable(
    title: String,
    columns: DbColumns,
    rows: DbRows,
    onAdd: (List<String>) -> Unit,
    onEdit: (List<String>) -> Unit,
    onDelete: (Long) -> Unit,
    content: @Composable () -> Unit)
{
    val focusManager = LocalFocusManager.current
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .border(1.dp, Color.Black)
            .padding(16.dp)
    ) {
        // Title
        Text(text = title)
        // Header
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Black)
        ) {
            for ((index, column) in columns.columns.withIndex()) {
                Text(
                    text = "${column["name"]} (${column["type"]}${if(column["limitation"] != null) "(${column["limitation"]})" else "" })",
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .background(if(index%2==0) Color.LightGray else Color.White)
                        .padding(2.dp)
                )
            }
            Text(
                text = "ACTIONS",
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(200.dp)
                    .background(Color.Cyan)
                    .padding(2.dp)
            )
        }
        // Rows
        Column {
            for (row in rows.rows) {
                var isEditing by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                        .height(if(isEditing)40.dp else 32.dp)
                ) {
                    var values = remember { MutableList(row.size) { "" }.toMutableStateList() }
                    val resetValues = {
                        for ((index, entry) in row.entries.withIndex()) {
                            values[index] = entry.value.toString()
                        }
                    }

                    for ((index, entry) in row.entries.withIndex()) {
                        if(isEditing) {
                            val isEnabled = columns.columns[index]["changeable"] == true || columns.columns[index]["changeable"] == null
                            if(columns.columns[index]["type"] == "timestamp" && isEnabled) {
                                if(values[index] == ""){
                                    values[index] = LocalDateTime.now().format(formatter).replace('T', ' ')
                                }
                                var rawDate by remember { mutableStateOf(values[index].filter { it.isDigit() }) }
                                fun formatDateTime(raw: String): String {
                                    if (raw.length < 4) return raw
                                    val year = raw.substring(0, 4)
                                    if (raw.length < 6) return "$year-${raw.substring(4)}"
                                    val month = raw.substring(4, 6)
                                    if (raw.length < 8) return "$year-$month-${raw.substring(6)}"
                                    val day = raw.substring(6, 8)
                                    if (raw.length < 10) return "$year-$month-$day ${raw.substring(8)}"
                                    val hour = raw.substring(8, 10)
                                    if (raw.length < 12) return "$year-$month-$day $hour:${raw.substring(10)}"
                                    val minute = raw.substring(10, 12)

                                    return "$year-$month-$day $hour:$minute"
                                }
                                BasicTextField(
                                    value = formatDateTime(rawDate),
                                    onValueChange = { newValue ->
                                        val digitsOnly = newValue.filter { it.isDigit() }
                                        if (digitsOnly.length <= 12) {
                                            try {
                                                formatter.parse(formatDateTime(digitsOnly + "000000000000") + ":00")
                                                values[index] = formatDateTime(digitsOnly + "000000000000") + ":00"
                                                rawDate = digitsOnly
                                            } catch (e: Exception) {}
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(8.dp)
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
                                        .padding(8.dp, 2.dp)
                                )
                            }
                            else if(columns.columns[index]["drop-down"] == null) {
                                BasicTextField(
                                    value = values[index],
                                    onValueChange = {
                                        values[index] = it
                                    },
                                    enabled = isEnabled,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(8.dp)
                                        .background(
                                            if (isEnabled) Color.White else Color.Gray,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
                                        .padding(8.dp, 2.dp)
                                )
                            } else {
                                var isExpanded by remember { mutableStateOf(false) }

                                Column(
                                    Modifier
                                        .padding(8.dp)
                                        .weight(1f)
                                        .height(23.dp)
                                        .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
                                        .background(Color.LightGray, RoundedCornerShape(8.dp))
                                ) {
                                    Text(
                                        text = values[index],
                                        modifier = Modifier
                                            .padding(8.dp, 2.dp)
                                            .fillMaxSize()
                                            .clickable {
                                                isExpanded = !isExpanded
                                            }
                                    )

                                    DropdownMenu(
                                        expanded = isExpanded,
                                        onDismissRequest = {
                                            isExpanded = false
                                        },
                                    ) {
                                        (columns.columns[index]["drop-down"] as? List<*>)?.let { optionsList ->
                                            for (option in optionsList) {
                                                DropdownMenuItem(
                                                    onClick = {
                                                        values[index] = option.toString()
                                                        isExpanded = false
                                                    }
                                                ) {
                                                    Text(option.toString())
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(
                                contentAlignment = Alignment.CenterStart,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .background(if (index % 2 == 0) Color.LightGray else Color.White)
                            ) {
                                Text(
                                    text = if(columns.columns[index]["type"] == "timestamp") entry.value.toString().replace('T', ' ')
                                    else entry.value.toString(),
                                    modifier = Modifier
                                        .padding(2.dp)
                                )
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .width(200.dp)
                            .fillMaxHeight()
                            .background(Color.Cyan)
                            .padding(2.dp)
                    ) {
                        if(isEditing){
                            Text(
                                "SAVE",
                                modifier = Modifier
                                    .background(Color.Green, RoundedCornerShape(8.dp))
                                    .clickable {
                                        onEdit(values)
                                        isEditing = false
                                    }
                                    .padding(8.dp, 2.dp)
                            )
                            Text(
                                "CANCEL",
                                modifier = Modifier
                                    .background(Color.Red, RoundedCornerShape(8.dp))
                                    .clickable {
                                        resetValues()
                                        isEditing = false
                                    }
                                    .padding(5.dp, 2.dp)

                            )
                        } else {
                            Text(
                                "EDIT",
                                modifier = Modifier
                                    .background(Color.Yellow, RoundedCornerShape(8.dp))
                                    .clickable {
                                        isEditing = true
                                        resetValues()
                                    }
                                    .padding(8.dp, 2.dp)
                            )
                            Text(
                                "DEL",
                                modifier = Modifier
                                    .background(Color.Red, RoundedCornerShape(8.dp))
                                    .clickable {
                                        onDelete(row["id"] as Long)
                                    }
                                    .padding(5.dp, 2.dp)

                            )
                        }
                    }
                }
            }
        }

        Divider(Modifier.weight(1f))
        Divider(Modifier.padding(16.dp))
        // Add new
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Black)
        ) {
            var values = remember { MutableList(columns.columns.size) { "" }.toMutableStateList() }
            for ((index, column) in columns.columns.withIndex()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (index % 2 == 0) Color.LightGray else Color.White)
                ) {
                    Text(
                        text = "${column["name"]} (${column["type"]}${if(column["limitation"] != null) "(${column["limitation"]})" else "" })",
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(2.dp)
                    )
                    val isEnabled = column["changeable"] == true || column["changeable"] == null
                    if(column["type"] == "timestamp" && isEnabled) {
                        if(values[index] == ""){
                            values[index] = LocalDateTime.now().format(formatter).replace('T', ' ')
                        }
                        var rawDate by remember { mutableStateOf(values[index].filter { it.isDigit() }) }
                        fun formatDateTime(raw: String): String {
                            if (raw.length < 4) return raw
                            val year = raw.substring(0, 4)
                            if (raw.length < 6) return "$year-${raw.substring(4)}"
                            val month = raw.substring(4, 6)
                            if (raw.length < 8) return "$year-$month-${raw.substring(6)}"
                            val day = raw.substring(6, 8)
                            if (raw.length < 10) return "$year-$month-$day ${raw.substring(8)}"
                            val hour = raw.substring(8, 10)
                            if (raw.length < 12) return "$year-$month-$day $hour:${raw.substring(10)}"
                            val minute = raw.substring(10, 12)

                            return "$year-$month-$day $hour:$minute"
                        }
                        BasicTextField(
                            value = formatDateTime(rawDate),
                            onValueChange = { newValue ->
                                val digitsOnly = newValue.filter { it.isDigit() }
                                if (digitsOnly.length <= 12) {
                                    try {
                                        formatter.parse(formatDateTime(digitsOnly + "000000000000") + ":00")
                                        values[index] = formatDateTime(digitsOnly + "000000000000") + ":00"
                                        rawDate = digitsOnly
                                    } catch (e: Exception) {}
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
                                .padding(8.dp, 2.dp)
                        )
                    }
                    else if(column["drop-down"] == null) {
                        BasicTextField(
                            value = values[index],
                            onValueChange = {
                                values[index] = it
                            },
                            enabled = isEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .background(if (isEnabled) Color.White else Color.Gray, RoundedCornerShape(8.dp))
                                .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
                                .padding(8.dp, 2.dp)
                        )
                    } else {
                        var isExpanded by remember { mutableStateOf(false) }

                        Column(
                            Modifier
                                .padding(8.dp)
                                .fillMaxWidth()
                                .height(23.dp)
                                .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
                                .background(Color.LightGray, RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = values[index],
                                modifier = Modifier
                                    .padding(8.dp, 2.dp)
                                    .fillMaxSize()
                                    .clickable {
                                        isExpanded = !isExpanded
                                    }
                            )

                            DropdownMenu(
                                expanded = isExpanded,
                                onDismissRequest = {
                                    isExpanded = false
                                },
                            ) {
                                (column["drop-down"] as? List<*>)?.let { optionsList ->
                                    for (option in optionsList) {
                                        DropdownMenuItem(
                                            onClick = {
                                                values[index] = option.toString()
                                                isExpanded = false
                                            }
                                        ) {
                                            Text(option.toString())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .background(Color.Cyan)
                    .width(200.dp)
                    .height(62.dp)
            ) {
                Text(
                    text = "ACTIONS",
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(200.dp)
                        .background(Color.Cyan)
                        .padding(2.dp)
                )
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxHeight()
                ) {
                    Text(
                        "ADD",
                        modifier = Modifier
                            .background(Color.Green, RoundedCornerShape(8.dp))
                            .clickable {
                                onAdd(values)
                            }
                            .padding(5.dp, 2.dp)
                    )
                }
            }
        }

        content()
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
        val rows = mutableListOf<Map<String, *>>()
        for(user in users) {
            rows.add(mapOf(
                "id" to user.id,
                "username" to user.username,
                "email" to user.email,
                "password_hash" to user.passwordHash
            ))
        }

        DbTable(
            "Користувачі",
            DbColumns(
                listOf(
                    mapOf(
                        "name" to "id",
                        "type" to "int",
                        "changeable" to false,
                    ),
                    mapOf(
                        "name" to "username",
                        "type" to "varchar",
                        "limitation" to "32",
                    ),
                    mapOf(
                        "name" to "email",
                        "type" to "text"
                    ),
                    mapOf(
                        "name" to "password_hash",
                        "type" to "text"
                    ),
                ),
            ),
            DbRows(rows),
            onAdd = { values: List<String> ->
                viewModel.addUser(UserDto(
                    id = null,
                    username = values[1],
                    email = values[2],
                    passwordHash = values[3]
                ))
            },
            onEdit = { values: List<String> ->
                viewModel.editUser(UserDto(
                    id = values[0].toLong(),
                    username = values[1],
                    email = values[2],
                    passwordHash = values[3]
                ))
            },
            onDelete = { id ->
                viewModel.deleteUser(id)
            }
        ) { }
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
        val rows = mutableListOf<Map<String, *>>()
        for(member in projectMembers) {
            rows.add(mapOf(
                "id" to member.id,
                "role" to member.role.toString(),
                "project_id" to member.projectId,
                "user_id" to member.userId,
            ))
        }

        DbTable(
            "Члени проєкту",
            DbColumns(
                listOf(
                    mapOf(
                        "name" to "id",
                        "type" to "int",
                        "changeable" to false,
                    ),
                    mapOf(
                        "name" to "role",
                        "type" to "enum",
                        "drop-down" to ProjectMemberRole.entries.map { it.name },
                    ),
                    mapOf(
                        "name" to "project_id",
                        "type" to "int",
                        "drop-down" to projects.map { it.id },
                    ),
                    mapOf(
                        "name" to "user_id",
                        "type" to "int",
                        "drop-down" to users.map { it.id }
                    )
                ),
            ),
            DbRows(rows),
            onAdd = { values: List<String> ->
                viewModel.addProjectMember(
                    ProjectMemberDto(
                        id = null,
                        role = ProjectMemberRole.valueOf(values[1]),
                        projectId = values[2].toLong(),
                        userId = values[3].toLong(),
                    )
                )
            },
            onEdit = { values: List<String> ->
                viewModel.editProjectMember(ProjectMemberDto(
                    id = values[0].toLong(),
                    role = ProjectMemberRole.valueOf(values[1]),
                    projectId = values[2].toLong(),
                    userId = values[3].toLong(),
                ))
            },
            onDelete = { id ->
                viewModel.deleteProjectMember(id)
            }
        ) { }
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
        val rows = mutableListOf<Map<String, *>>()
        for(project in projects) {
            rows.add(mapOf(
                "id" to project.id,
                "title" to project.title,
                "description" to project.description,
                "created_at" to project.createdAt?.format(formatter) ,
                "updated_at" to project.updatedAt?.format(formatter) ,
                "owner_id" to project.ownerId,
            ))
        }

        DbTable(
            "Проєкти",
            DbColumns(
                listOf(
                    mapOf(
                        "name" to "id",
                        "type" to "int",
                        "changeable" to false,
                    ),
                    mapOf(
                        "name" to "title",
                        "type" to "varchar",
                        "limitation" to "255",
                    ),
                    mapOf(
                        "name" to "description",
                        "type" to "text"
                    ),
                    mapOf(
                        "name" to "created_at",
                        "type" to "timestamp",
                        "changeable" to false,
                    ),
                    mapOf(
                        "name" to "updated_at",
                        "type" to "timestamp",
                        "changeable" to false,
                    ),
                    mapOf(
                        "name" to "owner_id",
                        "type" to "int",
                        "drop-down" to users.map { it.id },
                    ),
                ),
            ),
            DbRows(rows),
            onAdd = { values: List<String> ->
                viewModel.addProject(
                    ProjectDto(
                        id = null,
                        title = values[1],
                        description = values[2],
                        createdAt = null,
                        updatedAt = null,
                        ownerId = try { values[5].toLong() }catch(e:Exception) { 1 },
                    )
                )
            },
            onEdit = { values: List<String> ->
                viewModel.editProject(ProjectDto(
                    id = values[0].toLong(),
                    title = values[1],
                    description = values[2],
                    createdAt = LocalDateTime.parse(values[3], formatter),
                    updatedAt = LocalDateTime.parse(values[4], formatter),
                    ownerId = values[5].toLong(),
                ))
            },
            onDelete = { id ->
                viewModel.deleteProject(id)
            }
        ) { }
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
        val rows = mutableListOf<Map<String, *>>()
        for(board in boards) {
            rows.add(mapOf(
                "id" to board.id,
                "title" to board.title,
                "description" to board.description,
                "created_at" to board.createdAt?.format(formatter),
                "updated_at" to board.updatedAt?.format(formatter),
                "project_id" to board.projectId,
            ))
        }

        DbTable(
            "Дошки",
            DbColumns(
                listOf(
                    mapOf(
                        "name" to "id",
                        "type" to "int",
                        "changeable" to false,
                    ),
                    mapOf(
                        "name" to "title",
                        "type" to "varchar",
                        "limitation" to "255",
                    ),
                    mapOf(
                        "name" to "description",
                        "type" to "text"
                    ),
                    mapOf(
                        "name" to "created_at",
                        "type" to "timestamp",
                        "changeable" to false,
                    ),
                    mapOf(
                        "name" to "updated_at",
                        "type" to "timestamp",
                        "changeable" to false,
                    ),
                    mapOf(
                        "name" to "project_id",
                        "type" to "int",
                        "drop-down" to projects.map { it.id },
                    ),
                ),
            ),
            DbRows(rows),
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
                        createdAt = LocalDateTime.parse(values[3], formatter),
                        updatedAt = LocalDateTime.parse(values[4], formatter),
                        projectId = values[5].toLong(),
                    )
                )
            },
            onDelete = { id ->
                viewModel.deleteKanbanBoard(id)
            }
        ) { }
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
        val rows = mutableListOf<Map<String, *>>()
        for(column in columns) {
            rows.add(mapOf(
                "id" to column.id,
                "title" to column.title,
                "position" to column.position,
                "created_at" to column.createdAt?.format(formatter),
                "updated_at" to column.updatedAt?.format(formatter),
                "board_id" to column.boardId,
            ))
        }

        DbTable(
            "Стовпці",
            DbColumns(
                listOf(
                    mapOf(
                        "name" to "id",
                        "type" to "int",
                        "changeable" to false,
                    ),
                    mapOf(
                        "name" to "title",
                        "type" to "varchar",
                        "limitation" to "255",
                    ),
                    mapOf(
                        "name" to "position",
                        "type" to "int"
                    ),
                    mapOf(
                        "name" to "created_at",
                        "type" to "timestamp",
                        "changeable" to false,
                    ),
                    mapOf(
                        "name" to "updated_at",
                        "type" to "timestamp",
                        "changeable" to false,
                    ),
                    mapOf(
                        "name" to "board_id",
                        "type" to "int",
                        "drop-down" to boards.map { it.id },
                    ),
                ),
            ),
            DbRows(rows),
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
        ) { }
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
        val rows = mutableListOf<Map<String, *>>()
        for(task in tasks) {
            rows.add(mapOf(
                "id" to task.id,
                "title" to task.title,
                "description" to task.description,
                "position" to task.position,
                "priority" to task.priority,
                "due_date" to task.dueDate,
                "created_at" to task.createdAt?.format(formatter),
                "updated_at" to task.updatedAt?.format(formatter),
                "column_id" to task.columnId,
            ))
        }

        DbTable(
            "Завдання",
            DbColumns(
                listOf(
                    mapOf(
                        "name" to "id",
                        "type" to "int",
                        "changeable" to false,
                    ),
                    mapOf(
                        "name" to "title",
                        "type" to "varchar",
                        "limitation" to "255",
                    ),
                    mapOf(
                        "name" to "description",
                        "type" to "text"
                    ),
                    mapOf(
                        "name" to "position",
                        "type" to "int"
                    ),
                    mapOf(
                        "name" to "priority",
                        "type" to "enum",
                        "drop-down" to KanbanTaskPriorities.entries.map { it.name },
                    ),
                    mapOf(
                        "name" to "due_date",
                        "type" to "timestamp",
                    ),
                    mapOf(
                        "name" to "created_at",
                        "type" to "timestamp",
                        "changeable" to false,
                    ),
                    mapOf(
                        "name" to "updated_at",
                        "type" to "timestamp",
                        "changeable" to false,
                    ),
                    mapOf(
                        "name" to "column_id",
                        "type" to "int",
                        "drop-down" to columns.map { it.id },
                    ),
                ),
            ),
            DbRows(rows),
            onAdd = { values: List<String> ->
                viewModel.addKanbanTask(
                    KanbanTaskDto(
                        id = null,
                        title = values[1],
                        description = values[2],
                        position = values[3].toInt(),
                        priority = KanbanTaskPriorities.valueOf(values[4]),
                        dueDate = LocalDateTime.parse(values[5], formatter),
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
                        dueDate = LocalDateTime.parse(values[5], formatter),
                        createdAt = LocalDateTime.parse(values[6], formatter),
                        updatedAt = LocalDateTime.parse(values[7], formatter),
                        columnId = values[8].toLong(),
                    )
                )
            },
            onDelete = { id ->
                viewModel.deleteKanbanTask(id)
            }
        ) { }
    }
}
