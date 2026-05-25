package model;

import java.sql.Time;

/**
 * @author Javo
 */
public class Schedule {
    
    private String schedId;
    private String dayOfWeek;
    private Time startTime;   
    private Time endTime;   
    private int studentCount;  
    private String courseName; 

    // Constructors
    public Schedule(String schedId, String dayOfWeek, Time startTime, Time endTime, int studentCount) {
        this.schedId = schedId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.studentCount = studentCount;
    }

    public Schedule() {
    }

    public String getSchedId() {
        return schedId;
    }

    public void setSchedId(String schedId) {
        this.schedId = schedId;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Time getStartTime() {
        return startTime;
    }

    public void setStartTime(Time startTime) {
        this.startTime = startTime;
    }

    public Time getEndTime() {
        return endTime;
    }

    public void setEndTime(Time endTime) {
        this.endTime = endTime;
    }

    public int getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(int studentCount) {
        this.studentCount = studentCount;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }
}