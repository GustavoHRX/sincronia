package com.sincronia.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sincronia.app.R
import java.time.LocalDate

/** Navigation to the editor, passed down from the NavHost. */
class ItemActions(
    val openEvent: (localId: String) -> Unit,
    val openTask: (localId: String) -> Unit,
    val newItem: (date: LocalDate, type: String?, listId: String?) -> Unit,
)

@Composable
fun NewItemButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(onClick = onClick, modifier = modifier.padding(16.dp)) {
        Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.new_item))
    }
}
