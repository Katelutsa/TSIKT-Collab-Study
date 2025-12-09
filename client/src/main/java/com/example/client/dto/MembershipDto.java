package com.example.client.dto;

public class MembershipDto {

    private Long membershipId;
    private String role;
    private String joinedAt;   // ISO string
    private UserDto user;      // nested user object

    public MembershipDto() {}

    public Long getMembershipId() { return membershipId; }
    public void setMembershipId(Long membershipId) { this.membershipId = membershipId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getJoinedAt() { return joinedAt; }
    public void setJoinedAt(String joinedAt) { this.joinedAt = joinedAt; }

    public UserDto getUser() { return user; }
    public void setUser(UserDto user) { this.user = user; }
}
