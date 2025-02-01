package app.simplecloud.controller.shared.group

data class GroupTimeout(val timeoutDuration: Int, val timeoutBegin: Long = System.currentTimeMillis()) {

    fun isCooldownActive(): Boolean {
        val cooldownEndTime = timeoutBegin + timeoutDuration * 1000L
        return System.currentTimeMillis() < cooldownEndTime
    }

    fun remainingTimeInSeconds(): Long {
        val cooldownEndTime = timeoutBegin + timeoutDuration * 1000L
        val remainingTime = cooldownEndTime - System.currentTimeMillis()
        return if (remainingTime > 0) remainingTime / 1000 else 0
    }

}