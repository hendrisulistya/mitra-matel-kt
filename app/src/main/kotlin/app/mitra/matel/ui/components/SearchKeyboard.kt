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

@Composable
fun SearchKeyboard(
    keyboardLayout: KeyboardLayout = KeyboardLayout.QWERTY,
    onKeyClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        when (keyboardLayout) {
            KeyboardLayout.NUMERIC -> NumericKeyboard(onKeyClick)
            KeyboardLayout.QWERTY -> QwertyKeyboard(onKeyClick)
        }
    }
}

@Composable
fun NumericKeyboard(onKeyClick: (String) -> Unit) {
    val keys = listOf(
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        "*", "0", "#"
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(keys) { key ->
            Button(
                onClick = { onKeyClick(key) },
                modifier = Modifier
                    .aspectRatio(1.5f)
                    .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@Composable
fun QwertyKeyboard(onKeyClick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // First row: 1, 2, 3, ⌫
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            listOf("1", "2", "3").forEach { key ->
                Button(
                    onClick = { onKeyClick(key) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(1.dp, androidx.compose.ui.graphics.Color.Black),
                    shape = RoundedCornerShape(0.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = key, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Button(
                onClick = { onKeyClick("⌫") },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(1.dp, androidx.compose.ui.graphics.Color.Black),
                shape = RoundedCornerShape(0.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "⌫", fontSize = 48.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Second row: 4, 5, 6, ⌫
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            listOf("4", "5", "6").forEach { key ->
                Button(
                    onClick = { onKeyClick(key) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(1.dp, androidx.compose.ui.graphics.Color.Black),
                    shape = RoundedCornerShape(0.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = key, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Button(
                onClick = { onKeyClick("⌫") },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(1.dp, androidx.compose.ui.graphics.Color.Black),
                shape = RoundedCornerShape(0.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "⌫", fontSize = 48.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Third row: 7, 8, 9, 0
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            listOf("7", "8", "9", "0").forEach { key ->
                Button(
                    onClick = { onKeyClick(key) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(1.dp, androidx.compose.ui.graphics.Color.Black),
                    shape = RoundedCornerShape(0.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = key, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Fourth row: Q W E R T Y U I O P
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P").forEach { key ->
                Button(
                    onClick = { onKeyClick(key) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(1.dp, androidx.compose.ui.graphics.Color.Black),
                    shape = RoundedCornerShape(0.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            fontSize = 32.sp,
                            letterSpacing = 0.em,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Fifth row: A S D F G H J K L
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            listOf("A", "S", "D", "F", "G", "H", "J", "K", "L").forEach { key ->
                Button(
                    onClick = { onKeyClick(key) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(1.dp, androidx.compose.ui.graphics.Color.Black),
                    shape = RoundedCornerShape(0.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Sixth row: ⌫ Z X C V B N M ⌫
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Button(
                onClick = { onKeyClick("⌫") },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(1.dp, androidx.compose.ui.graphics.Color.Black),
                shape = RoundedCornerShape(0.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "⌫", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
            }
            listOf("Z", "X", "C", "V", "B", "N", "M").forEach { key ->
                Button(
                    onClick = { onKeyClick(key) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(1.dp, androidx.compose.ui.graphics.Color.Black),
                    shape = RoundedCornerShape(0.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Button(
                onClick = { onKeyClick("⌫") },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(1.dp, androidx.compose.ui.graphics.Color.Black),
                shape = RoundedCornerShape(0.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "⌫", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
            }
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
