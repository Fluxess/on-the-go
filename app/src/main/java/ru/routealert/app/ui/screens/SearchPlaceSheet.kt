package ru.routealert.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.routealert.app.location.PlaceHit
import ru.routealert.app.location.PlaceSearch
import ru.routealert.app.ui.theme.Glass
import ru.routealert.app.ui.theme.GlassBlue
import ru.routealert.app.ui.theme.liquidGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchPlaceSheet(
    onPick: (PlaceHit) -> Unit,
    onShowOnMap: (PlaceHit) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<PlaceHit>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun runSearch(q: String) {
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(380)
            if (q.trim().length < 2) {
                results = emptyList()
                error = null
                loading = false
                return@launch
            }
            loading = true
            error = null
            val hits = PlaceSearch.search(context, q)
            results = hits
            if (hits.isEmpty()) {
                error = "Ничего не найдено. Попробуйте «автовокзал Пермь» или 45.03, 38.97"
            }
            loading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xE61A1A1C),
        scrimColor = Color.Black.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp)
        ) {
            Text("Поиск точки", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(
                "Адрес, место или координаты",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f)
            )
            Spacer(modifier = Modifier.height(14.dp))

            SearchField(
                value = query,
                onValueChange = {
                    query = it
                    runSearch(it)
                },
                onClear = {
                    query = ""
                    results = emptyList()
                    error = null
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            when {
                loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = GlassBlue
                        )
                        Text("Ищем…", color = Color.White.copy(alpha = 0.75f))
                    }
                }
                error != null && results.isEmpty() -> {
                    Text(
                        error!!,
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 420.dp)
            ) {
                itemsIndexed(results) { index, hit ->
                    PlaceResultCard(
                        index = index + 1,
                        hit = hit,
                        onPick = { onPick(hit) },
                        onShowOnMap = { onShowOnMap(hit) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = shape, colors = Glass.MapFillStrong, stroke = Glass.Stroke)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    brush = Brush.verticalGradient(Glass.Selected),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Адрес или координаты",
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelMedium
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                cursorBrush = SolidColor(GlassBlue),
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 17.sp,
                    lineHeight = 22.sp
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            "Например: автовокзал Пермь",
                            color = Color.White.copy(alpha = 0.35f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    inner()
                }
            )
        }
        if (value.isNotEmpty()) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Очистить",
                tint = Color.White.copy(alpha = 0.55f),
                modifier = Modifier
                    .size(22.dp)
                    .clickable(onClick = onClear)
            )
        }
    }
}

@Composable
private fun PlaceResultCard(
    index: Int,
    hit: PlaceHit,
    onPick: () -> Unit,
    onShowOnMap: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = shape, colors = Glass.PanelFill, stroke = Glass.StrokeSoft)
            .clickable(onClick = onPick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color.White.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = index.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    hit.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    hit.subtitle,
                    color = Color.White.copy(alpha = 0.68f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onShowOnMap,
                modifier = Modifier
                    .weight(1f)
                    .liquidGlass(shape = RoundedCornerShape(16.dp), colors = Glass.Selected)
            ) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Точка на карте", color = Color.White)
            }
            TextButton(
                onClick = onPick,
                modifier = Modifier
                    .weight(1f)
                    .liquidGlass(
                        shape = RoundedCornerShape(16.dp),
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    )
            ) {
                Text("Выбрать", color = Color.White)
            }
        }
    }
}
