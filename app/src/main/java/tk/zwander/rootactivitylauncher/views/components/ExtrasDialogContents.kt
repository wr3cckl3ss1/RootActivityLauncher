package tk.zwander.rootactivitylauncher.views.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.data.ExtraType
import tk.zwander.rootactivitylauncher.util.findActionForComponent
import tk.zwander.rootactivitylauncher.util.findCategoriesForComponent
import tk.zwander.rootactivitylauncher.util.findDataForComponent
import tk.zwander.rootactivitylauncher.util.findExtrasForComponent
import tk.zwander.rootactivitylauncher.util.updateActionForComponent
import tk.zwander.rootactivitylauncher.util.updateCategoriesForComponent
import tk.zwander.rootactivitylauncher.util.updateDataForComponent
import tk.zwander.rootactivitylauncher.util.updateExtrasForComponent
import java.util.UUID

class ExtrasDialogModel(private val context: Context, private val componentKey: String) {
    val extras = mutableStateListOf<Pair<UUID, ExtraInfo>>().apply {
        addAll(context.findExtrasForComponent(componentKey).map {
            UUID.randomUUID() to it
        })
    }

    var action by mutableStateOf(context.findActionForComponent(componentKey))

    var data by mutableStateOf(context.findDataForComponent(componentKey))

    val categories = mutableStateListOf<Pair<UUID, String?>>().apply {
        addAll(context.findCategoriesForComponent(componentKey).map {
            UUID.randomUUID() to it
        })
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ExtrasDialog(
    componentKey: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val model = remember {
        ExtrasDialogModel(context, componentKey)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    context.updateExtrasForComponent(
                        componentKey,
                        model.extras.mapNotNull { if (it.second.key.isBlank()) null else it.second }
                    )
                    context.updateActionForComponent(componentKey, model.action)
                    context.updateDataForComponent(componentKey, model.data)
                    context.updateCategoriesForComponent(
                        componentKey,
                        model.categories.mapNotNull { it.second })
                }
            ) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        },
        modifier = modifier,
        title = {
            Text(text = stringResource(id = R.string.intent))
        },
        text = {
            ExtrasDialogContents(
                model = model,
                modifier = Modifier.fillMaxWidth(0.75f)
            )
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtrasDialogContents(
    model: ExtrasDialogModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = model.action,
            onValueChange = { model.action = it },
            label = {
                Text(text = stringResource(id = R.string.action))
            }
        )

        OutlinedTextField(
            value = model.data ?: "",
            onValueChange = {
                model.data = it.ifBlank { null }
            },
            label = {
                Text(text = stringResource(id = R.string.data))
            }
        )

        fun handleCategoryUpdate(id: UUID, index: Int, newValue: String?) {
            val isLast = model.categories.lastIndex == index

            when {
                newValue.isNullOrBlank() -> {
                    if (!isLast) {
                        model.categories.removeAll { it.first == id }
                    }
                }

                else -> {
                    model.categories.removeAt(index)
                    model.categories[index] = id to newValue

                    if (isLast) {
                        model.categories.add(UUID.randomUUID() to "")
                    }
                }
            }
        }

        if (model.categories.isEmpty()) {
            CategoryField(
                value = "",
                onValueChange = {
                    model.categories.add(UUID.randomUUID() to it)
                }
            )
        } else {
            model.categories.forEachIndexed { index, cat ->
                CategoryField(
                    value = cat.second ?: "",
                    onValueChange = {
                        handleCategoryUpdate(cat.first, index, it)
                    }
                )
            }
        }

        Spacer(Modifier.size(8.dp))
        Text(text = stringResource(id = R.string.extras))

        fun handleExtraUpdate(
            id: UUID,
            index: Int,
            newKey: String?,
            newType: ExtraType?,
            newValue: String?
        ) {
            val isLast = model.extras.lastIndex == index

            when {
                (newKey.isNullOrBlank() && newValue.isNullOrBlank() && newType == null) -> {
                    if (!isLast) {
                        model.extras.removeAll { it.first == id }
                    }
                }

                else -> {
                    val old = model.extras.removeAt(index)
                    model.extras[index] = id to ExtraInfo(
                        key = newKey ?: old.second.key,
                        value = newValue ?: old.second.value,
                        type = newType ?: old.second.type
                    )

                    if (isLast) {
                        model.extras.add(UUID.randomUUID() to ExtraInfo("", ""))
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (model.extras.isEmpty()) {
                item {
                    ExtraItem(
                        extraInfo = ExtraInfo("", "")
                    ) { key, type, value ->
                        model.extras.add(
                            UUID.randomUUID() to ExtraInfo(key ?: "", value ?: "", type)
                        )
                    }
                }
            } else {
                itemsIndexed(items = model.extras, key = { _, item -> item.first }) { index, item ->
                    ExtraItem(
                        extraInfo = item.second
                    ) { key, type, value ->
                        handleExtraUpdate(item.first, index, key, type, value)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(text = stringResource(id = R.string.category))
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun ExtraItem(
    extraInfo: ExtraInfo,
    onUpdate: (key: String?, type: ExtraType?, value: String?) -> Unit
) {
    var showingTypeDialog by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = extraInfo.key,
                onValueChange = {
                    onUpdate(it, null, null)
                },
                label = {
                    Text(text = stringResource(id = R.string.hint_key))
                },
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = extraInfo.safeType.nameRes.let { stringResource(id = it) },
                onValueChange = {},
                label = {
                    Text(text = stringResource(id = R.string.type))
                },
                modifier = Modifier
                    .clickable {
                        showingTypeDialog = true
                    }
                    .weight(1f),
                readOnly = true
            )
        }

        OutlinedTextField(
            value = extraInfo.value,
            onValueChange = {
                onUpdate(null, null, it)
            },
            label = {
                Text(text = stringResource(id = R.string.hint_value))
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showingTypeDialog) {
        AlertDialog(
            onDismissRequest = {
                showingTypeDialog = false
            },
            title = {
                Text(text = stringResource(id = R.string.type))
            },
            text = {
                ExtrasTypeDialogContents(
                    initial = extraInfo.safeType,
                    onTypeSelected = {
                        onUpdate(null, it, null)
                    },
                    modifier = Modifier.fillMaxWidth(0.75f)
                )
            },
            confirmButton = {
                TextButton(onClick = { showingTypeDialog = false }) {
                    Text(text = stringResource(id = android.R.string.cancel))
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        )
    }
}
