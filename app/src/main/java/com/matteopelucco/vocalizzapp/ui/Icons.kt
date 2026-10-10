package com.matteopelucco.vocalizzapp.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

// Icone Material (24 dp) definite a mano per non dipendere da material-icons-extended, molto pesante.

private fun icon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        addPath(PathParser().parsePathString(pathData).toNodes(), fill = SolidColor(Color.Black))
    }.build()

val CopyIcon: ImageVector = icon(
    "ContentCopy",
    "M16,1H4C2.9,1 2,1.9 2,3v14h2V3h12V1zM19,5H8C6.9,5 6,5.9 6,7v14c0,1.1 0.9,2 2,2h11c1.1,0 2,-0.9 2,-2V7C21,5.9 20.1,5 19,5zM19,21H8V7h11V21z",
)

val CheckIcon: ImageVector = icon(
    "Check",
    "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z",
)
