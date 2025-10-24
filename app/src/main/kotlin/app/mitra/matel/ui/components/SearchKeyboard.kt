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
    QWERTY
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

data class KeyboardLayoutDefinition(
    val rows: List<List<KeyboardKey>>
)

object KeyboardLayouts {
    val NUMERIC = KeyboardLayoutDefinition(
        rows = listOf(
            listOf(
                KeyboardKey("1", fontSize = 48),
                KeyboardKey("2", fontSize = 48),
                KeyboardKey("3", fontSize = 48)
            ),
            listOf(
                KeyboardKey("4", fontSize = 48),
                KeyboardKey("5", fontSize = 48),
                KeyboardKey("6", fontSize = 48)
            ),
            listOf(
                KeyboardKey("7", fontSize = 48),
                KeyboardKey("8", fontSize = 48),
                KeyboardKey("9", fontSize = 48)
            ),
            listOf(
                KeyboardKey("*", fontSize = 48),
                KeyboardKey("0", fontSize = 48),
                KeyboardKey("#", fontSize = 48)
            )
        )
    )
    
    val QWERTY = KeyboardLayoutDefinition(
        rows = listOf(
            listOf(
                KeyboardKey("1", fontSize = 48),
                KeyboardKey("2", fontSize = 48),
                KeyboardKey("3", fontSize = 48),
                KeyboardKey("⌫", KeyType.BACKSPACE, fontSize = 48)
            ),
            listOf(
                KeyboardKey("4", fontSize = 48),
                KeyboardKey("5", fontSize = 48),
                KeyboardKey("6", fontSize = 48),
                KeyboardKey("⌫", KeyType.BACKSPACE, fontSize = 48)
            ),
            listOf(
                KeyboardKey("7", fontSize = 48),
                KeyboardKey("8", fontSize = 48),
                KeyboardKey("9", fontSize = 48),
                KeyboardKey("0", fontSize = 48)
            ),
            listOf(
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
            ),
            listOf(
                KeyboardKey("A"),
                KeyboardKey("S"),
                KeyboardKey("D"),
                KeyboardKey("F"),
                KeyboardKey("G"),
                KeyboardKey("H"),
                KeyboardKey("J"),
                KeyboardKey("K"),
                KeyboardKey("L")
            ),
            listOf(
                KeyboardKey("⌫", KeyType.BACKSPACE),
                KeyboardKey("Z"),
                KeyboardKey("X"),
                KeyboardKey("C"),
                KeyboardKey("V"),
                KeyboardKey("B"),
                KeyboardKey("N"),
                KeyboardKey("M"),
                KeyboardKey("⌫", KeyType.BACKSPACE)
            )
        )
    )
}

@Composable
fun SearchKeyboard(
    keyboardLayout: KeyboardLayout = KeyboardLayout.QWERTY,
    onKeyClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val layoutDefinition = when (keyboardLayout) {
        KeyboardLayout.NUMERIC -> KeyboardLayouts.NUMERIC
        KeyboardLayout.QWERTY -> KeyboardLayouts.QWERTY
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
                    .weight(1f)
            ) {
                row.forEach { key ->
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
