package org.example.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.example.project.ui.pages.DbEditorPage
import org.example.project.viewmodels.DbEditorViewModel

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "kpz_kanban_lb",
    ) {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
        val dbEditorViewModel = DbEditorViewModel(client)
        DbEditorPage(dbEditorViewModel)
    }
}