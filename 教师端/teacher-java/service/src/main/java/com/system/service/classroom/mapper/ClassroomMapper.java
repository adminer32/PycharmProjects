package com.system.service.classroom.mapper;

import com.system.service.classroom.vo.ClassroomVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ClassroomMapper {

    List<ClassroomVO.LessonItem> getLessonsByClassId(@Param("classId") String classId, @Param("teacherId") Long teacherId);

    List<ClassroomVO.KnowledgePointVO> getKnowledgePointsByLessonId(@Param("lessonId") Long lessonId);

    List<ClassroomVO.NoteVO> getNotesByLessonId(@Param("lessonId") Long lessonId);

    int insertKnowledgePoint(@Param("input") ClassroomVO.AddKnowledgeInput input);

    int updateKnowledgeVideo(@Param("id") Long id, @Param("videoUrl") String videoUrl);

    int insertNote(@Param("input") ClassroomVO.AddNoteInput input);

    int deleteNotesByLessonId(@Param("lessonId") Long lessonId);

    int insertNoteBatch(@Param("lessonId") Long lessonId, @Param("notes") java.util.List<ClassroomVO.NoteItem> notes);

    Integer getTotalStudentsByLesson(@Param("videoId") Long videoId);

    Integer getActiveStudentsByLesson(@Param("videoId") Long videoId);
}
