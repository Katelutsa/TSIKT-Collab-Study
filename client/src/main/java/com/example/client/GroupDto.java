package com.example.client;

// Simple DTO matching backend Group JSON (only required fields)
public class GroupDto {

    private Long groupId;      // matches Group.getGroupId()
    private String name;
    private String description;

    public GroupDto() {
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

