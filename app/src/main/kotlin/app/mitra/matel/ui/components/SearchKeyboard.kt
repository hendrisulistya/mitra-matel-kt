package app.mitra.matel.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.em

enum class KeyboardLayout {
    NUMERIC,
    QWERTY1,
    QWERTY2
}

enum class KeyType {
    NORMAL,
    BACKSPACE,
    SPECIAL
}

data class KeyboardKey(
    val text: String,
    val type: KeyType = KeyType.NORMAL,
    val weight: Float = 1f,
    val fontSize: Int = 32
)

data class KeyboardRow(
    val keys: List<KeyboardKey>,
    val weight: Float = 1f
)

data class KeyboardLayoutDefinition(
    val rows: List<KeyboardRow>
)

object KeyboardLayouts {
    // Helper function to calculate relative keyboard height
    fun getKeyboardHeightRatio(layout: KeyboardLayout): Float {
        return when (layout) {
            KeyboardLayout.NUMERIC -> 1.0f // 4 rows, standard height
            KeyboardLayout.QWERTY1 -> 0.88f // 6 rows, 30% reduction from original 1.26f
            KeyboardLayout.QWERTY2 -> 0.8f // 4 rows, 20% shorter
        }
    }
    
    val NUMERIC = KeyboardLayoutDefinition(
        rows = listOf(
            KeyboardRow(listOf(
                KeyboardKey("1", fontSize = 48),
                KeyboardKey("2", fontSize = 48),
                KeyboardKey("3", fontSize = 48)
            )),
            KeyboardRow(listOf(
                KeyboardKey("4", fontSize = 48),
                KeyboardKey("5", fontSize = 48),
                KeyboardKey("6", fontSize = 48)
            )),
            KeyboardRow(listOf(
                KeyboardKey("7", fontSize = 48),
                KeyboardKey("8", fontSize = 48),
                KeyboardKey("9", fontSize = 48)
            )),
            KeyboardRow(listOf(
                KeyboardKey("*", fontSize = 48),
                KeyboardKey("0", fontSize = 48),
                KeyboardKey("#", fontSize = 48)
            ))
        )
    )
    
    val QWERTY1 = KeyboardLayoutDefinition(
        rows = listOf(
            KeyboardRow(listOf(
                KeyboardKey("1", fontSize = 36),
                KeyboardKey("2", fontSize = 36),
                KeyboardKey("3", fontSize = 36),
                KeyboardKey("⌫", KeyType.BACKSPACE, fontSize = 36)
            ), weight = 0.4f),
            KeyboardRow(listOf(
                KeyboardKey("4", fontSize = 36),
                KeyboardKey("5", fontSize = 36),
                KeyboardKey("6", fontSize = 36),
                KeyboardKey("⌫", KeyType.BACKSPACE, fontSize = 36)
            ), weight = 0.4f),
            KeyboardRow(listOf(
                KeyboardKey("7", fontSize = 36),
                KeyboardKey("8", fontSize = 36),
                KeyboardKey("9", fontSize = 36),
                KeyboardKey("0", fontSize = 36)
            ), weight = 0.4f),
            KeyboardRow(listOf(
                KeyboardKey("Q", fontSize = 28),
                KeyboardKey("W", fontSize = 28),
                KeyboardKey("E", fontSize = 28),
                KeyboardKey("R", fontSize = 28),
                KeyboardKey("T", fontSize = 28),
                KeyboardKey("Y", fontSize = 28),
                KeyboardKey("U", fontSize = 28),
                KeyboardKey("I", fontSize = 28),
                KeyboardKey("O", fontSize = 28),
                KeyboardKey("P", fontSize = 28)
            ), weight = 0.4f),
            KeyboardRow(listOf(
                KeyboardKey("A", fontSize = 28),
                KeyboardKey("S", fontSize = 28),
                KeyboardKey("D", fontSize = 28),
                KeyboardKey("F", fontSize = 28),
                KeyboardKey("G", fontSize = 28),
                KeyboardKey("H", fontSize = 28),
                KeyboardKey("J", fontSize = 28),
                KeyboardKey("K", fontSize = 28),
                KeyboardKey("L", fontSize = 28)
            ), weight = 0.4f),
            KeyboardRow(listOf(
                KeyboardKey("⌫", KeyType.BACKSPACE, fontSize = 28),
                KeyboardKey("Z", fontSize = 28),
                KeyboardKey("X", fontSize = 28),
                KeyboardKey("C", fontSize = 28),
                KeyboardKey("V", fontSize = 28),
                KeyboardKey("B", fontSize = 28),
                KeyboardKey("N", fontSize = 28),
                KeyboardKey("M", fontSize = 28),
                KeyboardKey("⌫", KeyType.BACKSPACE, fontSize = 28)
            ), weight = 0.4f)
        )
    )
    
    val QWERTY2 = KeyboardLayoutDefinition(
        rows = listOf(
            KeyboardRow(listOf(
                KeyboardKey("1"),
                KeyboardKey("2"),
                KeyboardKey("3"),
                KeyboardKey("4"),
                KeyboardKey("5"),
                KeyboardKey("6"),
                KeyboardKey("7"),
                KeyboardKey("8"),
                KeyboardKey("9"),
                KeyboardKey("0")
            ), weight = 0.7f),
            KeyboardRow(listOf(
                KeyboardKey("Q"),
                KeyboardKey("W"),
                KeyboardKey("E"),
                KeyboardKey("R"),
                KeyboardKey("T"),
                KeyboardKey("Y"),
                KeyboardKey("U"),
                KeyboardKey("I"),
                KeyboardKey("O"),
                KeyboardKey("P")
            ), weight = 0.7f),
            KeyboardRow(listOf(
                KeyboardKey("A"),
                KeyboardKey("S"),
                KeyboardKey("D"),
                KeyboardKey("F"),
                KeyboardKey("G"),
                KeyboardKey("H"),
                KeyboardKey("J"),
                KeyboardKey("K"),
                KeyboardKey("L")
            ), weight = 0.7f),
            KeyboardRow(listOf(
                KeyboardKey("⌫", KeyType.BACKSPACE),
                KeyboardKey("Z"),
                KeyboardKey("X"),
                KeyboardKey("C"),
                KeyboardKey("V"),
                KeyboardKey("B"),
                KeyboardKey("N"),
                KeyboardKey("M"),
                KeyboardKey("⌫", KeyType.BACKSPACE)
            ), weight = 1.0f)
        )
    )
}

@Composable
fun SearchKeyboard(
    keyboardLayout: KeyboardLayout = KeyboardLayout.QWERTY1,
    onKeyClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val layoutDefinition = when (keyboardLayout) {
        KeyboardLayout.NUMERIC -> KeyboardLayouts.NUMERIC
        KeyboardLayout.QWERTY1 -> KeyboardLayouts.QWERTY1
        KeyboardLayout.QWERTY2 -> KeyboardLayouts.QWERTY2
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        GenericKeyboard(
            layoutDefinition = layoutDefinition,
            onKeyClick = onKeyClick
        )
    }
}

@Composable
fun GenericKeyboard(
    layoutDefinition: KeyboardLayoutDefinition,
    onKeyClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        layoutDefinition.rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(row.weight)
            ) {
                row.keys.forEach { key ->
                    KeyboardButton(
                        key = key,
                        onKeyClick = onKeyClick,
                        modifier = Modifier
                            .weight(key.weight)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
fun KeyboardButton(
    key: KeyboardKey,
    onKeyClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = when (key.type) {
        KeyType.NORMAL -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        KeyType.BACKSPACE, KeyType.SPECIAL -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
    
    Button(
        onClick = { onKeyClick(key.text) },
        modifier = modifier.border(1.dp, androidx.compose.ui.graphics.Color.Black),
        shape = RoundedCornerShape(0.dp),
        contentPadding = PaddingValues(0.dp),
        colors = colors
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = key.text,
                fontSize = key.fontSize.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = 0.em
            )
        }
    }
}

@Preview
@Composable
fun SearchKeyboardPreview() {
    MaterialTheme {
        SearchKeyboard(
            onKeyClick = {},
            modifier = Modifier.height(300.dp)
        )
    }
}

@Preview
@Composable
fun NumericKeyboardPreview() {
    MaterialTheme {
        SearchKeyboard(
            keyboardLayout = KeyboardLayout.NUMERIC,
            onKeyClick = {},
            modifier = Modifier.height(300.dp)
        )
    }
}

@Preview
@Composable
fun Qwerty2KeyboardPreview() {
    MaterialTheme {
        SearchKeyboard(
            keyboardLayout = KeyboardLayout.QWERTY2,
            onKeyClick = {},
            modifier = Modifier.height(300.dp)
        )
    }
}
