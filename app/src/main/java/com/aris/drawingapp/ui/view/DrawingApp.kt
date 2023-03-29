package com.aris.drawingapp.ui.view

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.random.Random


// use
@SuppressLint("MutableCollectionMutableState")
@Composable
fun View() {

    val paths = remember { mutableStateOf(mutableListOf<PathState>()) }

    Scaffold(
        topBar = {
            com.aris.drawingapp.ui.view.TopAppBar {
                paths.value = mutableListOf()
            }
        },
        content = {

            DrawBody(paths)
        }
    )

}




// Data Class
data class PathState(
    var path: Path,
    var color: Color,
    var stroke: Float,
)


// Draw App
@Composable
fun TopAppBar(onDelete: () -> Unit) {
    TopAppBar(
        title = { Text(text = "Drawing App") },
        actions = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "")
            }
        }
    )
}

@SuppressLint("MutableCollectionMutableState")
@Composable
fun DrawBody(paths: MutableState<MutableList<PathState>>) {

    Box(modifier = Modifier.fillMaxSize()) {

        val drawColor = remember { mutableStateOf(Color.Black) }

        val drawBrush = remember { mutableStateOf(5f) }

        val usedColors = remember {
            mutableStateOf(mutableSetOf(Color.Black,
                Color.Red,
                Color.Gray))
        }

        paths.value.add(
            PathState(Path(), drawColor.value, drawBrush.value)
        )
        DrawingCanvas(drawColor , drawBrush  , usedColors  , paths.value )
        Tools(drawColor, usedColors.value, drawBrush)

    }
}


//Color Picker
@Composable
fun ColorPicker(onColorSelected: (Color) -> Unit) {

    Text(
        text = "Drag to Change Color",
        style = MaterialTheme.typography.subtitle1,
        modifier = Modifier.padding(8.dp)
    )

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenInPx = with(LocalDensity.current) { screenWidth.toPx() }

    var activeColor by remember { mutableStateOf(Color.Black) }

    val dragOffset = remember { mutableStateOf(0f) }

    Box(modifier = Modifier.padding(8.dp)) {
        Spacer(modifier = Modifier
            .height(10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(brush = Brush.horizontalGradient(
                colors = randomColorList(),
                startX = 0f,
                endX = screenInPx
            ))
            .align(Alignment.Center)
            .pointerInput("painter") {
                detectTapGestures { offset ->
                    dragOffset.value = offset.x
                    activeColor = getActiveColor(dragOffset.value, screenInPx)
                    onColorSelected.invoke(activeColor)
                }
            }
        )

        val min = 0.dp
        val max = screenWidth - 32.dp
        val (minPx, maxPx) = with(LocalDensity.current) { min.toPx() to max.toPx() }

        Icon(imageVector = Icons.Default.FiberManualRecord, contentDescription = null,
            tint = activeColor,
            modifier = Modifier
                .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                .border(
                    BorderStroke(4.dp, MaterialTheme.colors.onSurface),
                    shape = CircleShape
                )
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val newValue = dragOffset.value + delta
                        dragOffset.value = newValue.coerceIn(minPx, maxPx)
                        activeColor = getActiveColor(dragOffset.value, screenInPx)
                        onColorSelected.invoke(activeColor)
                    }
                )
        )
    }

}

fun randomColorList(): List<Color> {
    val colorList = mutableListOf<Color>()

    for (i in 0..360 step (2)) {
        val randomSaturation = 90 + Random.nextFloat() * 10
        val randomLightness = 50 + Random.nextFloat() * 10
        val hsv = android.graphics.Color.HSVToColor(
            floatArrayOf(
                i.toFloat(),
                randomSaturation,
                randomLightness
            )
        )
        colorList.add(Color(hsv))
    }

    return colorList

}

fun getActiveColor(dragPosition: Float, screenWith: Float): Color {
    val hue = (dragPosition / screenWith) * 360f
    val randomSaturation = 90 + Random.nextFloat() * 10
    val randomLightness = 50 + Random.nextFloat() * 10

    return Color(
        android.graphics.Color.HSVToColor(
            floatArrayOf(
                hue,
                randomSaturation,
                randomLightness
            )
        )
    )
}


// Tools
@Composable
fun Tools(
    drawColor: MutableState<Color>,
    usedColor: MutableSet<Color>,
    drawBrush: MutableState<Float>,
) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        // bar
        ColorPicker(onColorSelected = { color ->
            drawColor.value = color
        })

        //Stars
        Row(modifier = Modifier
            .horizontalGradientBackground(listOf(Color.Gray, Color.Black))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState())
            .animateContentSize()
        ) {
            usedColor.forEach {
                Icon(Icons.Default.Star, contentDescription = null,
                    tint = it,
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable { drawColor.value = it }
                )
            }
        }

        // نوار بازشونده
        var showBrushes by remember { mutableStateOf(false) }

        val strokes = remember { (1..50 step 5).toList() }

        FloatingActionButton(onClick = { showBrushes = !showBrushes },
            modifier = Modifier.padding(vertical = 4.dp)) {
            Icon(Icons.Default.Brush, contentDescription = null, tint = drawColor.value)
        }
        AnimatedVisibility(visible = showBrushes) {
            LazyColumn {
                items(strokes) {
                    IconButton(onClick = {
                        drawBrush.value = it.toFloat()
                        showBrushes = false
                    },
                        modifier = Modifier
                            .padding(8.dp)
                            .border(
                                border = BorderStroke(width = with(LocalDensity.current) { it.toDp() },
                                    color = Color.Gray),
                                shape = CircleShape)
                    ) {

                    }
                }
            }
        }

    }
}

//Stars
fun Modifier.horizontalGradientBackground(colors: List<Color>) =
    gradientBackGround(colors) { gradientColors, size ->
        Brush.horizontalGradient(
            colors = gradientColors,
            startX = 0f,
            endX = size.width
        )
    }


fun Modifier.gradientBackGround(
    colors: List<Color>,
    brushProvider: (List<Color>, Size) -> Brush,
): Modifier = composed {
    var size by remember { mutableStateOf(Size.Zero) }
    val gradient = remember(colors, size) { brushProvider(colors, size) }
    drawWithContent {
        size = this.size
        drawRect(brush = gradient)
        drawContent()
    }
}


//Drawing Canvas
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawingCanvas(
    drawColor: MutableState<Color>,
    drawBrush: MutableState<Float>,
    usedColors: MutableState<MutableSet<Color>>,
    paths: List<PathState>,
) {
    val currentPath = paths.last().path

    val movePath = remember { mutableStateOf<Offset?>(null) }

    Canvas(modifier = Modifier
        .fillMaxSize()
        .padding(top = 100.dp)
        .pointerInteropFilter {
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    currentPath.moveTo(it.x, it.y)
                    usedColors.value.add(drawColor.value)
                }
                MotionEvent.ACTION_MOVE -> {
                    movePath.value = Offset(it.x, it.y)
                }
                else -> {
                    movePath.value = null
                }
            }
            true
        }
    ) {
        movePath.value?.let {
            currentPath.lineTo(it.x, it.y)
            drawPath(
                path = currentPath,
                color = drawColor.value,
                style = Stroke(drawBrush.value)
            )
        }
        paths.forEach {
            drawPath(
                path = it.path,
                color = it.color,
                style = Stroke(it.stroke)
            )
        }
    }
}