package com.example.client;

// Item for ComboBox with group selection
public class GroupListItem {

    private Long groupId;
    private String name;

    public GroupListItem(Long groupId, String name) {
        this.groupId = groupId;
        this.name = name;
    }

    public Long getGroupId() {
        return groupId;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        // What will be displayed in ComboBox
        return name + " (id=" + groupId + ")";
    }
}

