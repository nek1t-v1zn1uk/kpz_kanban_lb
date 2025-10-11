package org.example.project.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import org.example.project.data.dtos.UserDto
import androidx.compose.runtime.State
import androidx.lifecycle.viewModelScope
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.util.reflect.typeInfo
import kotlinx.coroutines.launch
import org.example.project.data.dtos.KanbanBoardDto
import org.example.project.data.dtos.KanbanColumnDto
import org.example.project.data.dtos.KanbanTaskDto
import org.example.project.data.dtos.ProjectDto
import org.example.project.data.dtos.ProjectMemberDto

class DbEditorViewModel(
    private val client: HttpClient,
    private val baseUrl: String = "http://localhost:8082",
) : ViewModel() {


    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading


    private val _users = mutableStateOf<List<UserDto>>(emptyList())
    val users: State<List<UserDto>> = _users

    fun getAllUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            val response = client.get("$baseUrl/api/user")
            _users.value = response.body(typeInfo<List<UserDto>>())
            _isLoading.value = false
        }
    }
    fun addUser(userDto: UserDto) {
        viewModelScope.launch {
            val response = client.post("$baseUrl/api/user") {
                contentType(ContentType.Application.Json)
                setBody(userDto)
            }
            if (response.status.isSuccess()) {
                println("User created successfully. Status: ${response.status.value}")
                getAllUsers()
            } else {
                println("Failed to create user. Status: ${response.status.value}, Body: ${response.bodyAsText()}")
            }
        }
    }
    fun editUser(userDto: UserDto) {
        viewModelScope.launch {
            val response = client.put("$baseUrl/api/user") {
                contentType(ContentType.Application.Json)
                setBody(userDto)
            }
            if (response.status.isSuccess()) {
                println("User edited successfully. Status: ${response.status.value}")
                getAllUsers()
            } else {
                println("Failed to edit user. Status: ${response.status.value}, Body: ${response.bodyAsText()}")
            }
        }
    }
    fun deleteUser(id: Long) {
        viewModelScope.launch {
            val response = client.delete("$baseUrl/api/user/$id")
            if (response.status.isSuccess()) {
                println("User deleted successfully. Status: ${response.status.value}")
                getAllUsers()
            } else {
                println("Failed to delete user. Status: ${response.status.value}, Body: ${response.bodyAsText()}")
            }
        }
    }


    private val _projects = mutableStateOf<List<ProjectDto>>(emptyList())
    val projects: State<List<ProjectDto>> = _projects

    fun getAllProjects() {
        viewModelScope.launch {
            _isLoading.value = true
            val response = client.get("$baseUrl/api/project")
            _projects.value = response.body(typeInfo<List<ProjectDto>>())
            _isLoading.value = false
        }
    }
    fun addProject(projectDto: ProjectDto) {
        viewModelScope.launch {
            val response = client.post("$baseUrl/api/project") {
                contentType(ContentType.Application.Json)
                setBody(projectDto)
            }
            if (response.status.isSuccess()) {
                println("Project created successfully. Status: ${response.status.value}")
                getAllProjects()
            } else {
                println("Failed to create project. Status: ${response.status.value}, Body: ${response.bodyAsText()}")
            }
        }
    }
    fun editProject(projectDto: ProjectDto) {
        viewModelScope.launch {
            val response = client.put("$baseUrl/api/project") {
                contentType(ContentType.Application.Json)
                setBody(projectDto)
            }
            if (response.status.isSuccess()) {
                println("Project edited successfully. Status: ${response.status.value}")
                getAllProjects()
            } else {
                println("Failed to edit project. Status: ${response.status.value}, Body: ${response.bodyAsText()}")
            }
        }
    }
    fun deleteProject(id: Long) {
        viewModelScope.launch {
            val response = client.delete("$baseUrl/api/project/$id")
            if (response.status.isSuccess()) {
                println("Project deleted successfully. Status: ${response.status.value}")
                getAllProjects()
            } else {
                println("Failed to delete project. Status: ${response.status.value}, Body: ${response.bodyAsText()}")
            }
        }
    }


    private val _projectMembers = mutableStateOf<List<ProjectMemberDto>>(emptyList())
    val projectMembers: State<List<ProjectMemberDto>> = _projectMembers

    fun getAllProjectMembers() {
        viewModelScope.launch {
            _isLoading.value = true
            val response = client.get("$baseUrl/api/project-member")
            _projectMembers.value = response.body(typeInfo<List<ProjectMemberDto>>())
            _isLoading.value = false
        }
    }
    fun addProjectMember(projectMemberDto: ProjectMemberDto) {
        viewModelScope.launch {
            val response = client.post("$baseUrl/api/project-member") {
                contentType(ContentType.Application.Json)
                setBody(projectMemberDto)
            }
            if (response.status.isSuccess()) {
                println("Project member created successfully. Status: ${response.status.value}")
                getAllProjectMembers()
            } else {
                println("Failed to create project member. Status: ${response.status.value}, Body: ${response.bodyAsText()}")
            }
        }
    }
    fun editProjectMember(projectMemberDto: ProjectMemberDto) {
        viewModelScope.launch {
            val response = client.put("$baseUrl/api/project-member") {
                contentType(ContentType.Application.Json)
                setBody(projectMemberDto)
            }
            if (response.status.isSuccess()) {
                println("Project member edited successfully. Status: ${response.status.value}")
                getAllProjectMembers()
            } else {
                println("Failed to edit project member. Status: ${response.status.value}, Body: ${response.bodyAsText()}")
            }
        }
    }
    fun deleteProjectMember(id: Long) {
        viewModelScope.launch {
            val response = client.delete("$baseUrl/api/project-member/$id")
            if (response.status.isSuccess()) {
                println("Project member deleted successfully. Status: ${response.status.value}")
                getAllProjectMembers()
            } else {
                println("Failed to delete project member. Status: ${response.status.value}, Body: ${response.bodyAsText()}")
            }
        }
    }


    private val _kanbanBoards = mutableStateOf<List<KanbanBoardDto>>(emptyList())
    val kanbanBoards: State<List<KanbanBoardDto>> = _kanbanBoards

    fun getAllKanbanBoards() {
        viewModelScope.launch {
            _isLoading.value = true
            val response = client.get("$baseUrl/api/kanban-board")
            _kanbanBoards.value = response.body(typeInfo<List<KanbanBoardDto>>())
            _isLoading.value = false
        }
    }
    fun addKanbanBoard(kanbanBoardDto: KanbanBoardDto) {
        viewModelScope.launch {
            val response = client.post("$baseUrl/api/kanban-board") {
                contentType(ContentType.Application.Json)
                setBody(kanbanBoardDto)
            }
            if (response.status.isSuccess()) {
                println("Kanban board created successfully. Status: ${response.status.value}")
                getAllKanbanBoards()
            } else {
                println("Failed to create kanban board. Status: ${response.status.value}, Body: ${response.bodyAsText()}")
            }
        }
    }
    fun editKanbanBoard(kanbanBoardDto: KanbanBoardDto) {
        viewModelScope.launch {
            val response = client.put("$baseUrl/api/kanban-board") {
                contentType(ContentType.Application.Json)
                setBody(kanbanBoardDto)
            }
            if (response.status.isSuccess()) {
                println("Kanban board edited successfully. Status: ${response.status.value}")
                getAllKanbanBoards()
            } else {
                println("Failed to edit kanban board. Status: ${response.status.value}, Body: ${response.bodyAsText()}")
            }
        }
    }
    fun deleteKanbanBoard(id: Long) {
        viewModelScope.launch {
            val response = client.delete("$baseUrl/api/kanban-board/$id")
            if (response.status.isSuccess()) {
                println("Kanban board deleted successfully. Status: ${response.status.value}")
                getAllKanbanBoards()
            } else {
                println("Failed to delete kanban board. Status: ${response.status.value}, Body: ${response.bodyAsText()}")
            }
        }
    }


    private val _kanbanColumns = mutableStateOf<List<KanbanColumnDto>>(emptyList())
    val kanbanColumns: State<List<KanbanColumnDto>> = _kanbanColumns

    fun getAllKanbanColumns() {
        viewModelScope.launch {
            _isLoading.value = true
            val response = client.get("$baseUrl/api/kanban-column")
            _kanbanColumns.value = response.body(typeInfo<List<KanbanColumnDto>>())
            _isLoading.value = false
        }
    }
    fun addKanbanColumn(kanbanColumnDto: KanbanColumnDto) {
        viewModelScope.launch {
            val response = client.post("$baseUrl/api/kanban-column") {
                contentType(ContentType.Application.Json)
                setBody(kanbanColumnDto)
            }
            if (response.status.isSuccess()) {
                println("Kanban column created successfully. Status: ${response.status.value}")
                getAllKanbanColumns()
            } else {
                println("Failed to create kanban column. Status: ${response.status.value}, Body: ${response.bodyAsText()}")
            }
        }
    }
    fun editKanbanColumn(kanbanColumnDto: KanbanColumnDto) {
        viewModelScope.launch {
            val response = client.put("$baseUrl/api/kanban-column") {
                contentType(ContentType.Application.Json)
                setBody(kanbanColumnDto)
            }
            if (response.status.isSuccess()) {
                println("Kanban column edited successfully. Status: ${response.status.value}")
                getAllKanbanColumns()
            } else {
                println("Failed to edit kanban column. Status: ${response.status.value}, Body: ${response.bodyAsText()}")
            }
        }
    }
    fun deleteKanbanColumn(id: Long) {
        viewModelScope.launch {
            val response = client.delete("$baseUrl/api/kanban-column/$id")
            if (response.status.isSuccess()) {
                println("Kanban column deleted successfully. Status: ${response.status.value}")
                getAllKanbanColumns()
            } else {
                println("Failed to delete kanban column. Status: ${response.status.value}, Body: ${response.bodyAsText()}")
            }
        }
    }


    private val _kanbanTasks = mutableStateOf<List<KanbanTaskDto>>(emptyList())
    val kanbanTasks: State<List<KanbanTaskDto>> = _kanbanTasks

    fun getAllKanbanTasks() {
        viewModelScope.launch {
            _isLoading.value = true
            val response = client.get("$baseUrl/api/kanban-task")
            _kanbanTasks.value = response.body(typeInfo<List<KanbanTaskDto>>())
            _isLoading.value = false
        }
    }
    fun addKanbanTask(kanbanTaskDto: KanbanTaskDto) {
        viewModelScope.launch {
            val response = client.post("$baseUrl/api/kanban-task") {
                contentType(ContentType.Application.Json)
                setBody(kanbanTaskDto)
            }
            if (response.status.isSuccess()) {
                println("Kanban task created successfully. Status: ${response.status.value}")
                getAllKanbanTasks()
            } else {
                println("Failed to create kanban task. Status: ${response.status.value}, Body: ${response.bodyAsText()}")
            }
        }
    }
    fun editKanbanTask(kanbanTaskDto: KanbanTaskDto) {
        viewModelScope.launch {
            val response = client.put("$baseUrl/api/kanban-task") {
                contentType(ContentType.Application.Json)
                setBody(kanbanTaskDto)
            }
            if (response.status.isSuccess()) {
                println("Kanban task edited successfully. Status: ${response.status.value}")
                getAllKanbanTasks()
            } else {
                println("Failed to edit kanban task. Status: ${response.status.value}, Body: ${response.bodyAsText()}")
            }
        }
    }
    fun deleteKanbanTask(id: Long) {
        viewModelScope.launch {
            val response = client.delete("$baseUrl/api/kanban-task/$id")
            if (response.status.isSuccess()) {
                println("Kanban task deleted successfully. Status: ${response.status.value}")
                getAllKanbanTasks()
            } else {
                println("Failed to delete kanban task. Status: ${response.status.value}, Body: ${response.bodyAsText()}")
            }
        }
    }
}