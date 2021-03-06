package com.mindeurfou.database.course

import com.mindeurfou.utils.GBException
import com.mindeurfou.database.hole.HoleDbMapper
import com.mindeurfou.database.hole.HoleTable
import com.mindeurfou.model.course.outgoing.Course
import com.mindeurfou.model.course.outgoing.CourseDetails
import com.mindeurfou.model.course.outgoing.CourseDetailsMapper
import com.mindeurfou.model.course.incoming.PostCourseBody
import com.mindeurfou.model.course.incoming.PutCourseBody
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class CourseDaoImpl : CourseDao {

    override fun getCourseById(courseId: Int): CourseDetails? = transaction {

        val course = CourseTable.select {
            CourseTable.id eq courseId
        }.mapNotNull {
            CourseDbMapper.mapFromEntity(it)
        }.singleOrNull() ?: return@transaction null

        val holes = HoleTable.select {
            HoleTable.courseId eq courseId
        }.mapNotNull { resultRow ->
            HoleDbMapper.mapFromEntity(resultRow)
        }.sortedBy { it.holeNumber }

        CourseDetailsMapper.mapToCourseDetails(course, holes)
    }

    override fun insertCourse(postCourse: PostCourseBody): Int {
        return transaction {
            val courseId = CourseTable.insertAndGetId {
                it[name] = postCourse.name
                it[numberOfHoles] = postCourse.numberOfHOles
                it[par] = postCourse.par
                it[gamesPlayed] = 0
            }.value

            var index = 1
            HoleTable.batchInsert(postCourse.holes) { hole ->
                this[HoleTable.courseId] = courseId
                this[HoleTable.holeNumber] = index
                this[HoleTable.par] = hole
                index++
            }
            courseId
        }
    }

    override fun updateCourse(putCourse: PutCourseBody): CourseDetails {
        transaction {
            val updatedColumns = CourseTable.update({CourseTable.id eq putCourse.id}) {
                it[name] = putCourse.name
                it[numberOfHoles] = putCourse.numberOfHoles
                it[par] = putCourse.par
            }

            if (updatedColumns == 0) throw GBException(GBException.COURSE_NOT_FIND_MESSAGE)

            putCourse.holes.forEach { hole ->
                HoleTable.update( {HoleTable.id eq hole.id } ) {
                    it[par] = hole.par
                }
            }

        }
        return getCourseById(putCourse.id)!!
    }

    override fun deleteCourse(courseId: Int) = transaction {
        val columnDeleted = CourseTable.deleteWhere { CourseTable.id eq courseId }

        if (columnDeleted == 0) return@transaction false

        HoleTable.deleteWhere { HoleTable.courseId eq courseId }
        true
    }

    override fun getCourseByName(name: String): Course? = transaction {
        CourseTable.select {
            CourseTable.name eq name
        }.mapNotNull {
            CourseDbMapper.mapFromEntity(it)
        }.singleOrNull()
    }

    override fun getCourses(filters: Map<String, String>?, limit: Int, offset: Long): List<Course> {
        return transaction { getAllCourses(limit, offset) }
    }
    
    private fun getAllCourses(limit: Int, offset: Long) : List<Course> {
        return if (limit >= 1)
            CourseTable.selectAll()
                .limit(limit, offset)
                .orderBy(CourseTable.createdAt to SortOrder.DESC)
                .mapNotNull {  CourseDbMapper.mapFromEntity(it) }
        else
            CourseTable.selectAll()
                .orderBy(CourseTable.createdAt to SortOrder.DESC)
                .mapNotNull {  CourseDbMapper.mapFromEntity(it) }

    }

}