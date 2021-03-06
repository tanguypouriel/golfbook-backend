package com.mindeurfou.model.course.incoming

import com.mindeurfou.model.hole.local.Hole
import kotlinx.serialization.Serializable

@Serializable
data class PutCourseBody(
    val id: Int,
    val name : String,
    val numberOfHoles : Int,
    val par : Int,
    val stars : Int,
    val holes : List<Hole>
)
