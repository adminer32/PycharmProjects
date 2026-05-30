package com.system.service.classroom.service;

import com.system.service.classroom.vo.ClassroomVO;

import java.util.List;

public interface ClassroomService {

    List<ClassroomVO.LessonItem> getLessons(String classId, Long teacherId);

    List<ClassroomVO.KnowledgePointVO> getKnowledgePoints(Long lessonId);

    List<ClassroomVO.NoteVO> getNotes(Long lessonId);

    ClassroomVO.KnowledgePointVO addKnowledge(ClassroomVO.AddKnowledgeInput input);

    boolean updateKnowledgeVideo(Long id, String videoUrl);

    ClassroomVO.NoteVO addNote(ClassroomVO.AddNoteInput input);

    void saveNotes(ClassroomVO.SaveNotesInput input);

    ClassroomVO.AnalyzeResult analyzeVideo(ClassroomVO.AnalyzeInput input);
}
