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

val ShareIcon: ImageVector = icon(
    "Share",
    "M18,16.08c-0.76,0 -1.44,0.3 -1.96,0.77L8.91,12.7c0.05,-0.23 0.09,-0.46 0.09,-0.7s-0.04,-0.47 -0.09,-0.7l7.05,-4.11c0.54,0.5 1.25,0.81 2.04,0.81 1.66,0 3,-1.34 3,-3s-1.34,-3 -3,-3 -3,1.34 -3,3c0,0.24 0.04,0.47 0.09,0.7L8.04,9.81C7.5,9.31 6.79,9 6,9c-1.66,0 -3,1.34 -3,3s1.34,3 3,3c0.79,0 1.5,-0.31 2.04,-0.81l7.12,4.16c-0.05,0.21 -0.08,0.43 -0.08,0.65 0,1.61 1.31,2.92 2.92,2.92 1.61,0 2.92,-1.31 2.92,-2.92s-1.31,-2.92 -2.92,-2.92z",
)

val ChevronDownIcon: ImageVector = icon(
    "ExpandMore",
    "M16.59,8.59L12,13.17L7.41,8.59L6,10l6,6l6,-6z",
)

val ArrowBackIcon: ImageVector = icon(
    "ArrowBack",
    "M20,11H7.83l5.59,-5.59L12,4l-8,8l8,8l1.41,-1.41L7.83,13H20v-2z",
)

val CheckIcon: ImageVector = icon(
    "Check",
    "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z",
)
