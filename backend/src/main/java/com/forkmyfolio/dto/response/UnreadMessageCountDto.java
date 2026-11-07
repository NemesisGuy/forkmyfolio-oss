package com.forkmyfolio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for returning the count of unread messages.
 */
@Getter
@Setter
@AllArgsConstructor
public class UnreadMessageCountDto {

    /**
     * The total number of unread messages.
     */
    private long unreadCount;
}