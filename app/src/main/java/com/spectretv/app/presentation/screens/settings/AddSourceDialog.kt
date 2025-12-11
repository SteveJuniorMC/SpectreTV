package com.spectretv.app.presentation.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun AddSourceDialog(
    onDismiss: () -> Unit,
    onAddM3U: (name: String, url: String) -> Unit,
    onAddXtream: (name: String, serverUrl: String, username: String, password: String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // M3U fields
    var m3uName by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }

    // Xtream fields
    var xtreamName by remember { mutableStateOf("") }
    var xtreamServer by remember { mutableStateOf("") }
    var xtreamUsername by remember { mutableStateOf("") }
    var xtreamPassword by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Add Source",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("M3U") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Xtream") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> M3UForm(
                        name = m3uName,
                        url = m3uUrl,
                        onNameChange = { m3uName = it },
                        onUrlChange = { m3uUrl = it }
                    )
                    1 -> XtreamForm(
                        name = xtreamName,
                        serverUrl = xtreamServer,
                        username = xtreamUsername,
                        password = xtreamPassword,
                        onNameChange = { xtreamName = it },
                        onServerUrlChange = { xtreamServer = it },
                        onUsernameChange = { xtreamUsername = it },
                        onPasswordChange = { xtreamPassword = it }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            when (selectedTab) {
                                0 -> {
                                    if (m3uName.isNotBlank() && m3uUrl.isNotBlank()) {
                                        onAddM3U(m3uName, m3uUrl)
                                    }
                                }
                                1 -> {
                                    if (xtreamName.isNotBlank() && xtreamServer.isNotBlank() &&
                                        xtreamUsername.isNotBlank() && xtreamPassword.isNotBlank()) {
                                        onAddXtream(xtreamName, xtreamServer, xtreamUsername, xtreamPassword)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = when (selectedTab) {
                            0 -> m3uName.isNotBlank() && m3uUrl.isNotBlank()
                            1 -> xtreamName.isNotBlank() && xtreamServer.isNotBlank() &&
                                    xtreamUsername.isNotBlank() && xtreamPassword.isNotBlank()
                            else -> false
                        }
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@Composable
private fun M3UForm(
    name: String,
    url: String,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            placeholder = { Text("My Playlist") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            label = { Text("M3U URL") },
            placeholder = { Text("http://example.com/playlist.m3u") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
private fun XtreamForm(
    name: String,
    serverUrl: String,
    username: String,
    password: String,
    onNameChange: (String) -> Unit,
    onServerUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            placeholder = { Text("My IPTV Provider") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = serverUrl,
            onValueChange = onServerUrlChange,
            label = { Text("Server URL") },
            placeholder = { Text("http://provider.com:8080") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}
