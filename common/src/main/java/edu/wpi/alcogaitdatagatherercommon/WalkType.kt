package edu.wpi.alcogaitdatagatherercommon

/** Describes the supported gait trials and their display and wire-format names */
enum class WalkType(
    private val displayName: String,
    private val wireName: String,
) {
    NORMAL("NORMAL WALK", "NORMAL_WALK"),
    HEEL_TO_TOE("HEEL TO TOE", "HEEL_TO_TOE"),
    STANDING_ON_ONE_FOOT("STANDING ON ONE FOOT", "STANDING_ON_ONE_FOOT"),
    NYSTAGMUS("NYSTAGMUS", "NYSTAGMUS"),
    ;

    /** Returns the identifier sent between the phone and the wearable */
    fun toNoSpaceString(): String = wireName

    /** Returns the next configured walk type, or null after the final type */
    fun next(previousWalkType: WalkType?): WalkType? {
        val currentIndex = entries.indexOf(previousWalkType)
        return entries.getOrNull(currentIndex + 1)
    }

    override fun toString(): String = displayName
}
