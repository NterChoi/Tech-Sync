package com.techsync.dto;

import java.util.List;

/** 이름으로 찾은 가입 이메일(마스킹) 목록. */
public record FindIdResponse(List<String> emails) {
}
