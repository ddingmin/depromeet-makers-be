package com.depromeet.makers.domain.usecase

import com.depromeet.makers.domain.gateway.AttendanceGateway
import com.depromeet.makers.domain.gateway.NotificationGateway
import com.depromeet.makers.domain.gateway.SessionGateway
import com.depromeet.makers.domain.model.AttendanceStatus
import com.depromeet.makers.domain.model.Notification
import com.depromeet.makers.domain.model.NotificationType
import com.depromeet.makers.util.logger
import java.time.LocalDateTime

class UpdateAbsenceMember(
    private val sessionGateway: SessionGateway,
    private val attendanceGateway: AttendanceGateway,
    private val notificationGateway: NotificationGateway,
) : UseCase<UpdateAbsenceMember.UpdateAbsenceMemberInput, Unit> {
    data class UpdateAbsenceMemberInput(
        val today: LocalDateTime,
    )

    override fun execute(input: UpdateAbsenceMemberInput) {
        val logger = logger()

        val session = sessionGateway.findByStartTimeBetween(input.today, input.today.plusDays(1))
        if (session == null) {
            return
        }

        val attendances = attendanceGateway.findAllByGenerationAndWeek(session.generation, session.week)
        attendances
            .filter {
                it.attendanceStatus == AttendanceStatus.ATTENDANCE_ON_HOLD
            }
            .forEach {
                attendanceGateway.save(it.copy(attendanceStatus = AttendanceStatus.ABSENCE))
                notificationGateway.save(
                    Notification.newNotification(
                        memberId = it.member.memberId,
                        content = "출석 인증 시간이 초과되었습니다.\n" +
                                "출석 증빙은 담당 운영진에게 문의하세요.",
                        type = NotificationType.DOCUMENT,
                        createdAt = input.today,
                    )
                )
                logger.info("memberId: ${it.member.memberId} has been changed to ${AttendanceStatus.ATTENDANCE}")
            }
    }
}
